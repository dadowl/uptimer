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
            .add("servers", JsonBuilder().build())
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

    val uptimerItems = ArrayList<UptimerItem>()

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isNotEmpty() && args[0]=="--dev"){
            devMode = true
        }

        tg_token = Config(config.getJsonObject("Telegram")).getString("token")
        tg_username = Config(config.getJsonObject("Telegram")).getString("username")
        tg_channel = Config(config.getJsonObject("Telegram")).getLong("channel")
        tg_statusMessage = UptimerStatusMessage(Config(Config(config.getJsonObject("Telegram")).getJsonObject("status")))

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

        if (Config(config.getJsonObject("other")).getString("upMessage").isNotEmpty()){
            upMessage = Config(config.getJsonObject("other")).getString("upMessage")
        } else {
            UptimerLogger.info("Up message is empty in config. Use default message.")
        }
        if (Config(config.getJsonObject("other")).getString("downMessage").isNotEmpty()){
            downMessage = Config(config.getJsonObject("other")).getString("downMessage")
        } else {
            UptimerLogger.info("Down message is empty in config. Use default message.")
        }

        pingEvery = Config(config.getJsonObject("other")).getInt("pingEvery", 5)
        UptimerLogger.info("Ping servers every $pingEvery minute!")

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