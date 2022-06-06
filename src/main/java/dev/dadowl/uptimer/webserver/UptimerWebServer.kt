package dev.dadowl.uptimer.webserver

import com.google.gson.JsonArray
import dev.dadowl.uptimer.Uptimer
import dev.dadowl.uptimer.UptimerLogger
import dev.dadowl.uptimer.utils.JsonBuilder
import spark.Route
import spark.Service


class UptimerWebServer(val port: Int = 9000) {

    private val httpService = Service.ignite()

    fun start() {
        httpService.port(port)
        httpService.threadPool(350)
        httpService.internalServerError("Error: 500 internal error")

        httpService.get("/", Route { request, response ->
            response.type("application/json")

            val array = JsonArray()

            Uptimer.uptimerItems.forEach {
                array.add(it.toJson())
            }

            return@Route JsonBuilder().add("response", array).build()
        })
        UptimerLogger.info("Starting web-server on port $port")
    }

}