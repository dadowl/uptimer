package dev.dadowl.uptimer

import com.google.gson.JsonObject
import java.io.IOException
import java.net.*
import java.time.LocalDateTime


class UptimerItem(var ip: String, val serverName: String, val services: String,
                  val upMsg: String, val downMsg: String) {

    var status = PingStatus.ONLINE
    var downOn: LocalDateTime = LocalDateTime.now()
    var downTryes = 0

    enum class PingStatus(val icon: String){
        ONLINE("\uD83D\uDFE2"),
        OFFLINE("\uD83D\uDD34"),
        PENDING("\uD83D\uDFE1")
    }

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
        var online = true
         if (this.ip.startsWith("http")) {
            val connection: HttpURLConnection = URL(this.ip).openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
             connection.connectTimeout = 5000
            val responseCode: Int = connection.responseCode
            if (responseCode != 200) {
                online = false
            }
        } else if (this.ip.split(":").size > 1){
            val sockaddr: SocketAddress = InetSocketAddress(this.ip.split(":")[0], this.ip.split(":")[1].toInt())
            val socket = Socket()

            try {
                socket.connect(sockaddr, 5000)
            } catch (e: IOException){
               online = false
            }
        }  else {
            val geek = InetAddress.getByName(ip)
            online = geek.isReachable(5000)
        }

        if (!online){
            UptimerLogger.info("$ip is DOWN")
            if (this.status != PingStatus.OFFLINE)
                this.status = PingStatus.PENDING

            if (this.downTryes == 2) {
                Uptimer.uptimerTgNoticer.sendMessage(Uptimer.getMessage(downMsg, this))
                this.status = PingStatus.OFFLINE
            } else if (this.downTryes == 0) {
                downOn = LocalDateTime.now()
            }

            this.downTryes++
        }
        if (online && this.status != PingStatus.ONLINE){
            this.status = PingStatus.ONLINE
            Uptimer.uptimerTgNoticer.sendMessage(Uptimer.getMessage(upMsg, this))
            this.downTryes = 0
        }
        if (online && this.status == PingStatus.ONLINE) {
            UptimerLogger.info("$ip is UP")
        }
    }

}