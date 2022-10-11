package dev.dadowl.uptimer

import com.google.gson.JsonObject
import dev.dadowl.uptimer.events.UptimerEventType
import dev.dadowl.uptimer.events.UptimerPingEvent
import dev.dadowl.uptimer.utils.JsonBuilder
import java.io.IOException
import java.net.*
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit


class UptimerItem(
    var value: String,
    val serverName: String,
    val services: String,
    val type: UptimerItemType
) {

    companion object {
        fun getMessage(msg: String, item: UptimerItem): String {
            var message = msg

            if (message.contains("{ip}")) {
                message = message.replace("{ip}", item.value)
            }
            if (message.contains("{serverName}")) {
                message = message.replace("{serverName}", item.serverName)
            }
            if (message.contains("{services}")) {
                message = message.replace("{services}", item.services)
            }
            if (message.contains("{downTime}")) {
                val diff = ChronoUnit.SECONDS.between(item.downOn, LocalDateTime.now())
                message = message.replace("{downTime}", diff.toString())
            }
            if (message.contains("{errorCode}")) {
                message = message.replace("{errorCode}", item.errorCode.toString())
            }
            if (message.contains("{status}")) {
                message = message.replace("{status}", item.status.icon)
            }

            return message
        }
    }

    var group = "servers"
    var status = PingStatus.ONLINE
    var downOn: LocalDateTime = LocalDateTime.now()
    var downTryes = 0
    var errorCode = 0

    var upMsg: String = ""
    var downMsg: String = ""

    enum class PingStatus(val icon: String) {
        ONLINE("\uD83D\uDFE2"),
        OFFLINE("\uD83D\uDD34"),
        PENDING("\uD83D\uDFE1")
    }

    constructor(json: JsonObject, value: String, type: UptimerItemType) : this(
        value,
        json.get("serverName").asString,
        json.get("services").asString,
        type
    ) {
        if (json.get("group") != null && json.get("group").asString.isNotEmpty()) {
            group = json.get("group").asString
        }

        upMsg = if (json.asJsonObject.get("upMessage") != null && json.asJsonObject.get("upMessage").asString.isNotEmpty()) {
            json.asJsonObject.get("upMessage").asString
        } else {
            var temp = Uptimer.getDefaultUpMessageForGroup(group)
            if (temp.isEmpty()) temp = Uptimer.upMessage
            temp
        }
        downMsg = if (json.asJsonObject.get("downMessage") != null && json.asJsonObject.get("downMessage").asString.isNotEmpty()) {
            json.asJsonObject.get("downMessage").asString
        } else {
            var temp = Uptimer.getDefaultDownMessageForGroup(group)
            if (temp.isEmpty()) temp = Uptimer.downMessage
            temp
        }
    }

    fun toStringMain(): String {
        return "UptimerItem(group = $group, value = $value, type: ${type}, services = $services)"
    }

    override fun toString(): String {
        return "UptimerItem(group = $group, value = $value, type: ${type},  services = $services, status = $status, upMsg = $upMsg, downMsg = $downMsg)"
    }

    fun toJson(hideIp: Boolean = true): JsonObject {
        val builder = JsonBuilder()
            .add("group", group)

        if (!hideIp) {
            builder.add("ip", this.value)
        }

        builder
            .add("services", this.services)
            .add("status", this.status.toString())

        return builder.build()
    }

    fun ping() {
        UptimerLogger.info("PING $value")
        var online = true

        when(this.type){
            UptimerItemType.SITE -> {
                val connection: HttpURLConnection = URL(this.value).openConnection() as HttpURLConnection
                connection.requestMethod = "HEAD"
                connection.connectTimeout = 5000
                connection.setRequestProperty("User-Agent", "Uptimer(https://github.com/dadowl/uptimer)")
                val responseCode: Int = connection.responseCode
                if (responseCode != 200) {
                    online = false
                    errorCode = responseCode
                }
            }
            UptimerItemType.HOST, UptimerItemType.IP -> {
                if (this.value.split(":").size > 1) {
                    val sockaddr: SocketAddress = InetSocketAddress(this.value.split(":")[0], this.value.split(":")[1].toInt())
                    val socket = Socket()

                    try {
                        socket.connect(sockaddr, 5000)
                    } catch (e: IOException) {
                        online = false
                    }
                } else {
                    val geek = InetAddress.getByName(value)
                    online = geek.isReachable(5000)
                }
            }
        }

        if (!online) {
            UptimerLogger.info("$value is DOWN")
            if (this.status != PingStatus.OFFLINE)
                this.status = PingStatus.PENDING
            Uptimer.notifyListeners(UptimerPingEvent(this, UptimerEventType.PING_PENDING))

            if (this.downTryes == (Uptimer.downTryes - 1)) {
                this.status = PingStatus.OFFLINE
                Uptimer.notifyListeners(UptimerPingEvent(this, UptimerEventType.PING_OFFLINE))
            }

            if (this.downTryes == 0) {
                downOn = LocalDateTime.now()
            }

            this.downTryes++
        }
        if (online) {
            if (this.status == PingStatus.OFFLINE) {
                this.status = PingStatus.ONLINE
                this.downTryes = 0
                this.errorCode = 0
                Uptimer.notifyListeners(UptimerPingEvent(this, UptimerEventType.PING_ONLINE))
            }
            if (this.status == PingStatus.PENDING) {
                this.status = PingStatus.ONLINE
                this.downTryes = 0
                this.errorCode = 0
            }
            if (this.status == PingStatus.ONLINE) {
                UptimerLogger.info("$value is UP")
            }
        }
    }
}