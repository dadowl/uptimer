package dev.dadowl.uptimer.webserver

import com.google.gson.JsonArray
import dev.dadowl.uptimer.Uptimer
import dev.dadowl.uptimer.UptimerLogger
import dev.dadowl.uptimer.utils.JsonBuilder
import spark.Route
import spark.Service


class UptimerWebServer(private val port: Int = 9000, private val hideIp: Boolean = true) {

    private val httpService = Service.ignite()

    fun start() {
        httpService.port(port)
        httpService.threadPool(350)
        httpService.internalServerError("Error: 500 internal error")

        httpService.get("/", Route { request, response ->
            response.type("application/json")

            val jsonBuilder = JsonBuilder()
            val serversJson = JsonBuilder()

            val groupList = Uptimer.uptimerItems.map { it.group }
            for (group in groupList) {
                val items = Uptimer.uptimerItems.filter { it.group == group }
                val servers = JsonArray()
                items.forEach { servers.add(it.toJson(hideIp)) }
                serversJson.add(group, servers).build()
            }

            return@Route jsonBuilder
                .add("response",
                    JsonBuilder()
                        .add("status", Uptimer.getItemsStatus())
                        .add("items", serversJson.build())
                    .build())
                .build()
        })
        UptimerLogger.info("Starting web-server on port $port")
    }

}