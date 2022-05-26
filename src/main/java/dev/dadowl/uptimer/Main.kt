package dev.dadowl.uptimer

import com.google.gson.JsonArray
import dev.dadowl.uptimer.utils.Config
import dev.dadowl.uptimer.utils.FileUtil
import dev.dadowl.uptimer.utils.JsonBuilder
import kotlin.system.exitProcess

object Main {

    val defaultConfig =
        JsonBuilder()
            .add("Telegram",
                JsonBuilder()
                    .add("token", "")
                    .add("username", "")
                .build()
            )
            .add("items", JsonBuilder().build())
        .build()

    private var config = Config(FileUtil.openFile("config.json", defaultConfig))

    val uptimerItems = ArrayList<UptimerItem>()

    @JvmStatic
    fun main(args: Array<String>) {
        loadUptimerItems()
    }

    fun loadUptimerItems(){
        UptimerLogger.info("Load UptimerItems")
        var jarray: JsonArray

        try {
            jarray = config.getJsonArray("items")
        } catch (ex: Exception){
            UptimerLogger.warn("No items found.")
            UptimerLogger.warn("Stopping...")
            exitProcess(0)
        }

        if (jarray.isEmpty) {
            UptimerLogger.warn("No items found.")
            UptimerLogger.warn("Stopping...")
            exitProcess(0)
        }

        jarray.forEach{ item ->
            val it = UptimerItem(item.asJsonObject)
            uptimerItems.add(it)
            UptimerLogger.info("Loaded ${it.toStringMain()}")
        }
    }
}