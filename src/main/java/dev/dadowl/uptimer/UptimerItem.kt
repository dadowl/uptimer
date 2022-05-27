package dev.dadowl.uptimer

import com.google.gson.JsonObject
import java.net.InetAddress
import java.time.LocalDateTime


class UptimerItem(var ip: String, val serverName: String, val services: String,
                  val upMsg: String, val downMsg: String) {

    var status = true
    var downOn: LocalDateTime = LocalDateTime.now()
    var downTryes = 0

    constructor(json: JsonObject)
            : this(json.get("ip").asString, json.get("serverName").asString, json.get("services").asString,
                json.get("upMessage").asString, json.get("downMessage").asString)

    fun toStringMain(): String{
        return "UptimerItem(ip = $ip, services = $services)"
    }

    override fun toString(): String{
        return "UptimerItem(ip = $ip, services = $services, status = $status)"
    }

    fun ping(){
        UptimerLogger.info("PING $ip")
        val geek = InetAddress.getByName(ip)
        val query = geek.isReachable(5000)
        if (!query){
            UptimerLogger.info("$ip is DOWN")
            this.status = false

            if (this.downTryes == 2) {
                Uptimer.uptimerTgNoticer.sendMessage(Uptimer.getMessage(downMsg, this))
            } else if (this.downTryes == 0) {
                downOn = LocalDateTime.now()
            }

            this.downTryes++
        }
        if (query && !this.status){
            this.status = true
            Uptimer.uptimerTgNoticer.sendMessage(Uptimer.getMessage(upMsg, this))
            this.downTryes = 0
        }
        if (query && this.status) {
            UptimerLogger.info("$ip is UP")
        }
    }

}