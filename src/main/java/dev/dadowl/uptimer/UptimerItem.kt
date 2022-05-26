package dev.dadowl.uptimer

import com.google.gson.JsonObject

class UptimerItem(val ip: String, val services: String) {

    var status = false

    constructor(json: JsonObject) : this(json.get("ip").asString, json.get("services").asString)

    fun toStringMain(): String{
        return "UptimerItem(ip = $ip, services = $services)"
    }

    override fun toString(): String{
        return "UptimerItem(ip = $ip, services = $services, status = $status)"
    }


}