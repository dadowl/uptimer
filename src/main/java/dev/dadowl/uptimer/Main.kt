package dev.dadowl.uptimer

import com.google.gson.JsonArray
import dev.dadowl.uptimer.utils.Config
import dev.dadowl.uptimer.utils.FileUtil
import dev.dadowl.uptimer.utils.JsonBuilder
import dev.dadowl.uptimer.utils.Utils
import kotlin.system.exitProcess

object Main {

    val defaultConfig =
        JsonBuilder()
            .add("Telegram",
                JsonBuilder()
                    .add("token", "")
                    .add("username", "")
                    .add("channel", 0)
                .build()
            )
            .add("items", JsonBuilder().build())
        .build()

    private var config = Config(FileUtil.openFile("config.json", defaultConfig))

    private var tg_token = ""
    private var tg_username = ""
    private var tg_channel = 0L
    private lateinit var uptimerTgNoticer: UptimerTgNoticer

    val uptimerItems = ArrayList<UptimerItem>()

    @JvmStatic
    fun main(args: Array<String>) {
        tg_token = Config(config.getJsonObject("Telegram")).getString("token")
        tg_username = Config(config.getJsonObject("Telegram")).getString("username")
        tg_channel = Config(config.getJsonObject("Telegram")).getLong("channel")

        if (tg_token.isEmpty() || tg_username.isEmpty() || tg_channel == 0L){
            stop("Telegram settings error.")
        }

        uptimerTgNoticer = UptimerTgNoticer(tg_token, tg_username, tg_channel)

        //uptimerTgNoticer.sendMessage("Hello World!")

        loadUptimerItems()
    }

    fun loadUptimerItems(){
        UptimerLogger.info("Load UptimerItems")
        var jarray = JsonArray()

        try {
            jarray = config.getJsonArray("items")
        } catch (ex: Exception){
            stop("No items found.")
        }

        if (jarray.isEmpty) {
            stop("No items found.")
        }

        jarray.forEach{ item ->
            val it = UptimerItem(item.asJsonObject)
            if (Utils.isValidIp(it.ip)){
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
}