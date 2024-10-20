package dev.dadowl.uptimer

import com.google.gson.JsonObject
import dev.dadowl.uptimer.events.UptimerEventType
import dev.dadowl.uptimer.events.UptimerPingEvent
import dev.dadowl.uptimer.utils.Config
import dev.dadowl.uptimer.utils.JsonBuilder
import dev.dadowl.uptimer.utils.Utils
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

    var group = "servers"
    var status = PingStatus.ONLINE
    var downOn: LocalDateTime? = null
    private var downTryes = 0
    var httpStatusCode = 0

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
        val itemConfig = Config(json)

        if (itemConfig.getString("group", "").isNotEmpty()) {
            group = itemConfig.getString("group", "servers")
        }

        upMsg = itemConfig.getString("upMessage", "").ifEmpty {
            var defaultUpMessage = Uptimer.getDefaultUpMessageForGroup(group)
            if (defaultUpMessage.isEmpty()) defaultUpMessage = Uptimer.defaultUpMessage

            defaultUpMessage
        }
        downMsg = itemConfig.getString("downMessage", "").ifEmpty {
            var defaultDownMessage = Uptimer.getDefaultDownMessageForGroup(group)
            if (defaultDownMessage.isEmpty()) defaultDownMessage = Uptimer.defaultDownMessage

            defaultDownMessage
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

        val online: Boolean = when (this.type) {
            UptimerItemType.SITE -> {
                val connection: HttpURLConnection = URL(this.value).openConnection() as HttpURLConnection
                connection.requestMethod = "HEAD"
                connection.connectTimeout = 5000
                connection.setRequestProperty("User-Agent", "Uptimer(https://github.com/dadowl/uptimer)")
                val responseCode: Int = connection.responseCode
                if (responseCode != 200) {
                    httpStatusCode = responseCode

                    false
                } else {
                    true
                }
            }

            UptimerItemType.HOST, UptimerItemType.IP -> {
                if (this.value.split(":").size > 1) {
                    val sockaddr: SocketAddress =
                        InetSocketAddress(this.value.split(":")[0], this.value.split(":")[1].toInt())
                    val socket = Socket()

                    try {
                        socket.connect(sockaddr, 5000)
                        true
                    } catch (e: IOException) {
                        false
                    } finally {
                        socket.close()
                    }
                } else {
                    val geek = InetAddress.getByName(value)

                    geek.isReachable(5000)
                }
            }
        }

        if (!online) {
            if (this.status == PingStatus.ONLINE) {
                this.status = PingStatus.PENDING

                Uptimer.fireEventAsync(UptimerPingEvent(this, UptimerEventType.PING_PENDING))
            }

            this.downTryes++

            if (this.downTryes >= Uptimer.downTryes && downOn == null) {
                this.status = PingStatus.OFFLINE
                downOn = LocalDateTime.now()

                Uptimer.fireEventAsync(UptimerPingEvent(this, UptimerEventType.PING_OFFLINE))
            }
        } else {
            if (this.status == PingStatus.OFFLINE) {
                clearItem()

                Uptimer.fireEventAsync(UptimerPingEvent(this, UptimerEventType.PING_ONLINE))
            }
            if (this.status == PingStatus.PENDING) {
                clearItem()
            }
        }

        UptimerLogger.info(
        "$value is ${if (this.status == PingStatus.ONLINE) "UP" else "DOWN"}. " +
            "Current status: ${this.status}. " +
            when (this.status) {
                PingStatus.PENDING -> "Try: $downTryes"
                PingStatus.OFFLINE -> "Down on: $downOn"
                else -> ""
            }
        )
    }

    private fun clearItem() {
        this.status = PingStatus.ONLINE
        this.downTryes = 0
        this.httpStatusCode = 0
        this.downOn = null
    }

    fun formatMessage(msg: String): String {
        val diff = if (this.downOn != null) ChronoUnit.SECONDS.between(this.downOn, LocalDateTime.now()) else ""

        return Utils.replacePlaceholders(
            msg, hashMapOf(
                "{ip}" to this.value,
                "{serverName}" to this.serverName,
                "{services}" to this.services,
                "{downTime}" to diff.toString(),
                "{errorCode}" to this.httpStatusCode.toString(),
                "{status}" to this.status.icon
            )
        )
    }
}