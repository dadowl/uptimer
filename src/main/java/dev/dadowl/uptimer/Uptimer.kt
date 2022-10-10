package dev.dadowl.uptimer

import com.coreoz.wisp.Scheduler
import com.coreoz.wisp.schedule.Schedules
import com.google.gson.JsonArray
import dev.dadowl.uptimer.events.UptimerPingEvent
import dev.dadowl.uptimer.events.UptimerEventListener
import dev.dadowl.uptimer.noticers.UptimerMailNoticer
import dev.dadowl.uptimer.noticers.UptimerTgNoticer
import dev.dadowl.uptimer.utils.*
import dev.dadowl.uptimer.webserver.UptimerWebServer
import java.time.Duration
import java.util.*
import kotlin.collections.ArrayList
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

    var upMessage = "Server {serverName}({ip}) is UP!"
    var downMessage = "Server {serverName}({ip}) is DOWN!"
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
            upMessage = config.getString("upMessage")
        } else {
            UptimerLogger.warn("Up message is empty in config. Use default message.")
        }
        if (config.getString("downMessage").isNotEmpty()){
            downMessage = config.getString("downMessage")
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

        loadUptimerItems()

        scheduler.schedule({
                uptimerItems.forEach { it.ping() }
                uptimerTgNoticer.updateStatusMessage()
            },
            Schedules.afterInitialDelay(Schedules.fixedDelaySchedule(pingValue), Duration.ZERO)
        )

        if (config.getBoolean("WebServer.enable", true)) {
            uptimerWebServer.start()
        } else {
            UptimerLogger.info("The web server is disabled.")
        }
    }

    fun loadUptimerItems(){
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
            val it = UptimerItem(item.asJsonObject)
            var corrected = false
            if (it.ip.startsWith("http")) {
                if (Utils.isValidURL(it.ip))
                    corrected = true
            } else if (it.ip.split(":").size > 1){
                if (Utils.isValidIp(it.ip.split(":")[0]))
                    corrected = true
            } else {
                if (Utils.isValidIp(it.ip))
                    corrected = true
            }
            if (corrected){
                uptimerItems.add(it)
                UptimerLogger.info("Loaded ${it}")
            } else {
                UptimerLogger.warn("Skipped - ${it.toStringMain()} - wrong IP!")
            }
        }

        if (uptimerItems.isEmpty()){
            stop("No items found.")
        }
    }

    fun getItemsStatus(): String {
        return if (uptimerItems.filter { it.status == UptimerItem.PingStatus.ONLINE }.size == uptimerItems.size){
            "allOnline"
        } else if (uptimerItems.filter { it.status == UptimerItem.PingStatus.OFFLINE }.size == uptimerItems.size){
            "allOffline"
        } else {
            "someOffline"
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

    fun notifyListeners(event: UptimerPingEvent){
        eventListeners.forEach{ it.onPingEvent(event) }
    }

    fun getDefaultUpMessageForGroup(group: String): String{
        return config.getString("groupsDefaultMessages.$group.upMessage")
    }

    fun getDefaultDownMessageForGroup(group: String): String{
        return config.getString("groupsDefaultMessages.$group.downMessage")
    }
}