package dev.dadowl.uptimer

import com.coreoz.wisp.Scheduler
import com.coreoz.wisp.schedule.Schedules
import com.google.gson.JsonArray
import dev.dadowl.uptimer.utils.*
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.system.exitProcess

object Uptimer {

    private val scheduler = Scheduler()

    private val defaultConfig =
        JsonBuilder()
            .add("other",
                JsonBuilder()
                    .add("pingEvery", 5)
                    .add("downTryes", 3)
                    .add("upMessage", "Server {serverName}({ip}) is UP!")
                    .add("downMessage", "Server {serverName}({ip}) is DOWN!")
                .build()
            )
            .add("Telegram",
                JsonBuilder()
                    .add("token", "")
                    .add("username", "")
                    .add("channel", -1)
                    .add("status",
                        JsonBuilder()
                            .add("msgId", -1)
                            .add("lines",
                                JsonArrayBuilder()
                                    .add("{status}")
                                    .add("")
                                    .add("Servers:")
                                    .add("{servers}")
                                .build()
                            )
                            .add("serverPattern", "{status} - {serverName} - {services}")
                            .add("status",
                                JsonBuilder()
                                    .add("allOnline","\uD83D\uDFE2 All servers are online!")
                                    .add("allOffline","\uD83D\uDD34 All servers are offline!")
                                    .add("someOffline","\uD83D\uDFE1 Some servers are offline!")
                                .build()
                            )
                        .build()
                    )
                .build()
            )
            .add("servers",
                JsonArrayBuilder()
                    .add(
                        JsonBuilder()
                            .add("ip", "8.8.8.8")
                            .add("serverName", "Example server")
                            .add("services", "Google DNS")
                            .add("upMessage", "Server {serverName}({ip}) is UP!  It was offline {downTime} seconds!")
                        .build()
                    )
                .build()
            )
        .build()

    private var config = Config(FileUtil.openFile("config.json", defaultConfig))

    var devMode = false

    private var tg_token = ""
    private var tg_username = ""
    private var tg_channel = 0L
    private lateinit var tg_statusMessage: UptimerStatusMessage
    lateinit var uptimerTgNoticer: UptimerTgNoticer

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

        tg_token = config.getString("Telegram.token")
        tg_username = config.getString("Telegram.username")
        tg_channel = config.getLong("Telegram.channel")
        tg_statusMessage = UptimerStatusMessage(Config(config.getJsonObject("Telegram.status")))

        if (tg_token.isEmpty() || tg_username.isEmpty() ||  tg_channel == -1L){
            stop("Telegram settings error.")
        }

        if (tg_statusMessage.id == -1){
            UptimerLogger.warn("Status message id is 0! Ignoring this function.")
        }

        uptimerTgNoticer = UptimerTgNoticer(tg_token, tg_username, tg_channel, tg_statusMessage)
        uptimerTgNoticer.connect()

        if (devMode){
            uptimerTgNoticer.sendMessage("Status message", true)
            stop()
        }

        if (config.getString("Telegram.upMessage").isNotEmpty()){
            upMessage = config.getString("Telegram.upMessage")
        } else {
            UptimerLogger.info("Up message is empty in config. Use default message.")
        }
        if (config.getString("Telegram.downMessage").isNotEmpty()){
            downMessage = config.getString("Telegram.downMessage")
        } else {
            UptimerLogger.info("Down message is empty in config. Use default message.")
        }

        pingEvery = config.getInt("Telegram.pingEvery", pingEvery)
        UptimerLogger.info("Ping servers every $pingEvery minute!")
        downTryes = config.getInt("Telegram.downTryes", downTryes)
        UptimerLogger.info("The server will be considered offline after $downTryes failed ping attempts.")

        loadUptimerItems()

        scheduler.schedule({
                uptimerItems.forEach { it.ping() }
                uptimerTgNoticer.updateStatusMessage()
            },
            Schedules.afterInitialDelay(Schedules.fixedDelaySchedule(Duration.ofMinutes(pingEvery.toLong())), Duration.ZERO)
        )
    }

    fun loadUptimerItems(){
        UptimerLogger.info("Load UptimerItems")
        var jarray = JsonArray()

        try {
            jarray = config.getJsonArray("servers")
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

    fun getMessage(msg: String, item: UptimerItem): String{
        var message = msg

        if(message.contains("{ip}")){
            message = message.replace("{ip}", item.ip)
        }
        if(message.contains("{serverName}")){
            message = message.replace("{serverName}", item.serverName)
        }
        if(message.contains("{services}")){
            message = message.replace("{services}", item.services)
        }
        if(message.contains("{downTime}")){
            val diff = ChronoUnit.SECONDS.between(item.downOn, LocalDateTime.now())
            message = message.replace("{downTime}", diff.toString())
        }

        return message
    }
}