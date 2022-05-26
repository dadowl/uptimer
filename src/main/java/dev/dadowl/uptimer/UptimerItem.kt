package dev.dadowl.uptimer

import com.google.gson.JsonObject
import java.net.InetAddress
import java.time.LocalDateTime


class UptimerItem(val ip: String, val serverName: String, val services: String) {

    var status = false
    var downOn: LocalDateTime = LocalDateTime.now()

    constructor(json: JsonObject) : this(json.get("ip").asString, json.get("serverName").asString, json.get("services").asString)

    fun toStringMain(): String{
        return "UptimerItem(ip = $ip, services = $services)"
    }

    override fun toString(): String{
        return "UptimerItem(ip = $ip, services = $services, status = $status)"
    }

    fun ping(){
        val geek = InetAddress.getByName(ip)
        println("Sending Ping Request to $ip")
        if (!geek.isReachable(5000)) {
            this.status = false
            downOn = LocalDateTime.now()
            Main.uptimerTgNoticer.sendMessage(Main.getMessage(Main.downMessage, this))
        }
        if (geek.isReachable(5000) && !this.status){
            this.status = true
            Main.uptimerTgNoticer.sendMessage(Main.getMessage(Main.upMessage, this))
        }
    }

}