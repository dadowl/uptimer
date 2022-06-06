package dev.dadowl.uptimer

import com.coreoz.wisp.Scheduler
import com.coreoz.wisp.schedule.Schedules
import com.google.gson.JsonArray
import dev.dadowl.uptimer.noticers.UptimerTgNoticer
import dev.dadowl.uptimer.utils.*
import dev.dadowl.uptimer.webserver.UptimerWebServer
import java.time.Duration
import java.util.*
import kotlin.collections.ArrayList
import kotlin.system.exitProcess

object Uptimer {

    private val scheduler = Scheduler()

    private var config = Config(FileUtil.openFile("config.json", DefaultConfig.DEFAULT.json))
    private var noticersConfig = Config(FileUtil.openFile("noticers.json", DefaultConfig.NOTICERS.json))
    private var serversConfig = Config(FileUtil.openFile("servers.json", DefaultConfig.SERVERS.json))

    var devMode = false

    val uptimerWebServer = UptimerWebServer(config.getInt("WebServer.port", 9000))

    val uptimerTgNoticer: UptimerTgNoticer = UptimerTgNoticer(Config(noticersConfig.getJsonObject("Telegram")))

    var upMessage = "Server {serverName}({ip}) is UP!"
    var downMessage = "Server {serverName}({ip}) is DOWN!"
    var pingEvery = 5
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

        if (config.getString("upMessage").isNotEmpty()){
            upMessage = config.getString("upMessage")
        } else {
            UptimerLogger.info("Up message is empty in config. Use default message.")
        }
        if (config.getString("downMessage").isNotEmpty()){
            downMessage = config.getString("downMessage")
        } else {
            UptimerLogger.info("Down message is empty in config. Use default message.")
        }

        pingEvery = config.getInt("pingEvery", pingEvery)
        UptimerLogger.info("Ping servers every $pingEvery minute!")
        downTryes = config.getInt("downTryes", downTryes)
        UptimerLogger.info("The server will be considered offline after $downTryes failed ping attempts.")

        loadUptimerItems()

        scheduler.schedule({
                uptimerItems.forEach { it.ping() }
                uptimerTgNoticer.updateStatusMessage()
            },
            Schedules.afterInitialDelay(Schedules.fixedDelaySchedule(Duration.ofMinutes(pingEvery.toLong())), Duration.ZERO)
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
            if (item.asJsonObject.get("upMessage") == null) item.asJsonObject.addProperty("upMessage", upMessage)
            if (item.asJsonObject.get("downMessage") == null) item.asJsonObject.addProperty("downMessage", downMessage)

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
                UptimerLogger.info("Loaded ${it.toStringMain()}")
            } else {
                UptimerLogger.warn("Skipped - ${it.toStringMain()} - wrong IP!")
            }
        }

        if (uptimerItems.isEmpty()){
            stop("No items found.")
        }
    }

    fun stop(){
        UptimerLogger.warn("Stopping...")
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
}