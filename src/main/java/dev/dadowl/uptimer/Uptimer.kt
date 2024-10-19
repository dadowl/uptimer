package dev.dadowl.uptimer

import com.coreoz.wisp.Scheduler
import com.coreoz.wisp.schedule.Schedules
import com.google.gson.JsonArray
import dev.dadowl.uptimer.events.UptimerCheckCompletedEvent
import dev.dadowl.uptimer.events.UptimerEventListener
import dev.dadowl.uptimer.events.UptimerPingEvent
import dev.dadowl.uptimer.noticers.UptimerMailNoticer
import dev.dadowl.uptimer.noticers.UptimerTgNoticer
import dev.dadowl.uptimer.utils.Config
import dev.dadowl.uptimer.utils.FileUtil
import dev.dadowl.uptimer.utils.Utils
import dev.dadowl.uptimer.webserver.UptimerWebServer
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.Duration
import java.util.*
import java.util.concurrent.Executors
import kotlin.system.exitProcess

object Uptimer {

    val scheduler = Scheduler()

    private val eventListeners = LinkedList<UptimerEventListener>()

    private var config = Config(FileUtil.openFile("config.json", DefaultConfig.DEFAULT.json))
    private var noticersConfig = Config(FileUtil.openFile("noticers.json", DefaultConfig.NOTICERS.json))
    private var serversConfig = Config(FileUtil.openFile("servers.json", DefaultConfig.SERVERS.json))

    var devMode = false

    val uptimerWebServer = UptimerWebServer(config.getInt("WebServer.port", 9000), config.getBoolean("WebServer.hideIp", true))

    val uptimerTgNoticer: UptimerTgNoticer = UptimerTgNoticer(Config(noticersConfig.getJsonObject("Telegram")))
    val uptimerMailNoticer = UptimerMailNoticer(Config(noticersConfig.getJsonObject("mail")))

    var defaultUpMessage = "Server {serverName}({ip}) is UP!"
    var defaultDownMessage = "Server {serverName}({ip}) is DOWN!"
    var pingEvery = "5m"
    var downTryes = 3

    val uptimerItems = ArrayList<UptimerItem>()

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isNotEmpty() && args[0]=="--dev"){
            devMode = true
        }

        uptimerTgNoticer.connect()

        if (devMode){
            if (uptimerTgNoticer.enabled){
                if (uptimerTgNoticer.statusMessage.id != -1) {
                    UptimerLogger.warn("The status message id is currently installed. You sure to update status message id? [Y/N]")
                    val scan = Scanner(System.`in`)
                    when (scan.next().lowercase()) {
                        "N", "n", "no" -> {
                            stop()
                        }
                    }
                }
                uptimerTgNoticer.sendMessage("Status message", true)
            }
            stop()
        }

        addEventListener(uptimerTgNoticer)
        addEventListener(uptimerMailNoticer)

        if (config.getString("upMessage").isNotEmpty()){
            defaultUpMessage = config.getString("upMessage")
        } else {
            UptimerLogger.warn("Up message is empty in config. Use default message.")
        }
        if (config.getString("downMessage").isNotEmpty()){
            defaultDownMessage = config.getString("downMessage")
        } else {
            UptimerLogger.warn("Down message is empty in config. Use default message.")
        }

        downTryes = config.getInt("downTryes", downTryes)
        if (downTryes <= 0) downTryes = 1
        UptimerLogger.info("The server will be considered offline after $downTryes failed ping attempts.")

        pingEvery = config.getString("pingEvery", pingEvery)
        if(pingEvery.isEmpty()) pingEvery = "5m"

        var pingVal = pingEvery.substring(0, pingEvery.length - 1).toLong()
        if (pingVal <= 0) pingVal = 1

        val pingValue = if (pingEvery.contains("h")){
            Duration.ofHours(pingVal)
        } else if (pingEvery.contains("s")) {
            Duration.ofSeconds(pingVal)
        } else {
            Duration.ofMinutes(pingVal)
        }

        UptimerLogger.info("Ping servers every $pingVal${Utils.lastChar(pingEvery)}!")

        if (config.getBoolean("WebServer.enable", true)) {
            uptimerWebServer.start()
        } else {
            UptimerLogger.info("The web server is disabled.")
        }

        loadUptimerItems()

        scheduler.schedule(
            {
                this.executePingAndNotify(uptimerItems)
            },
            Schedules.afterInitialDelay(Schedules.fixedDelaySchedule(pingValue), Duration.ZERO)
        )
    }

    private fun loadUptimerItems(){
        UptimerLogger.info("Load UptimerItems")
        var jarray = JsonArray()

        try {
            jarray = serversConfig.getJsonArray("servers")
        } catch (ex: Exception){
            stop("No items found.")
        }

        if (jarray.isEmpty) {
            stop("No items found.")
        }

        jarray.forEach{ item ->
            val jsonData = Config(item.asJsonObject)

            val (value, type) = when {
                jsonData.getString("host").isNotEmpty() -> jsonData.getString("host") to UptimerItemType.HOST
                jsonData.getString("site").isNotEmpty() -> jsonData.getString("site") to UptimerItemType.SITE
                else -> jsonData.getString("ip") to UptimerItemType.IP
            }

            if (type.isValid(value)) {
                val it = UptimerItem(item.asJsonObject, value, type)
                uptimerItems.add(it)
                UptimerLogger.info("Loaded ${it.toStringMain()}")
            } else {
                UptimerLogger.warn("Skipped - $type:$value - wrong IP!")
            }
        }

        if (uptimerItems.isEmpty()){
            stop("No items found.")
        }
    }

    fun getItemsStatus(): String {
        return when {
            uptimerItems.all { it.status == UptimerItem.PingStatus.ONLINE } -> "allOnline"
            uptimerItems.all { it.status == UptimerItem.PingStatus.OFFLINE } -> "allOffline"
            else -> "someOffline"
        }
    }

    fun stop(){
        UptimerLogger.info("Stopping...")
        exitProcess(0)
    }

    fun stop(err: String){
        UptimerLogger.error(err)
        stop()
    }

    fun saveStatusId(id: Int){
        noticersConfig.json.getAsJsonObject("Telegram").get("status").asJsonObject.addProperty("msgId", id)
        FileUtil.saveFile("noticers.json", noticersConfig.json)
    }

    fun addEventListener(listener: UptimerEventListener){
        eventListeners.add(listener)
    }

    fun fireEvent(event: EventObject){
        eventListeners.forEach {
            if (event is UptimerPingEvent){
                it.onPingEvent(event)
            }
            if (event is UptimerCheckCompletedEvent){
                it.onCheckCompleted(event)
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun fireEventAsync(event: EventObject) {
        GlobalScope.launch(Dispatchers.IO) {
            fireEvent(event)
        }
    }

    fun getDefaultUpMessageForGroup(group: String): String{
        return config.getString("groupsDefaultMessages.$group.upMessage")
    }

    fun getDefaultDownMessageForGroup(group: String): String{
        return config.getString("groupsDefaultMessages.$group.downMessage")
    }

    private fun executePingAndNotify(uptimerItems: List<UptimerItem>) {
        val executorService = Executors.newFixedThreadPool(uptimerItems.size) { runnable ->
            Thread(runnable).apply {
                isDaemon = true
                name = "PingerThread"
            }
        }

        try {
            val futures = uptimerItems.map { item ->
                executorService.submit {
                    item.ping()
                }
            }

            futures.forEach { it.get() }

            fireEventAsync(UptimerCheckCompletedEvent(uptimerItems))
        } finally {
            executorService.shutdown()
        }
    }
}