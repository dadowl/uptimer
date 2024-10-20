package dev.dadowl.uptimer

import com.google.gson.JsonObject
import dev.dadowl.uptimer.utils.JsonArrayBuilder
import dev.dadowl.uptimer.utils.JsonBuilder

enum class DefaultConfig(val json: JsonObject) {

    DEFAULT(
        JsonBuilder()
            .add("pingEvery", "5m")
            .add("downTryes", 3)
            .add("WebServer",
                JsonBuilder()
                    .add("enable", true)
                    .add("port", 9000)
                    .add("hideIp", true)
                .build()
            )
            .add("upMessage", "Server {serverName}({ip}) is UP!")
            .add("downMessage", "Server {serverName}({ip}) is DOWN!")
            .add("groupsDefaultMessages",
                JsonBuilder()
                    .add("group-name", JsonBuilder()
                        .add("upMessage", "example up message for group")
                        .add("downMessage", "example down message for group")
                    .build())
                .build())
        .build()
    ),
    NOTICERS(
        JsonBuilder()
            .add(
                "Telegram",
                JsonBuilder()
                    .add("enabled", false)
                    .add("token", "")
                    .add("username", "")
                    .add("channel", -1)
                    .add("deleteAfter", "1h")
                    .add("sendNotifications", true)
                    .add("status",
                        JsonBuilder()
                            .add("msgId", -1)
                            .add(
                                "lines",
                                JsonArrayBuilder()
                                    .add("{status}")
                                    .add("")
                                    .add("Servers:")
                                    .add("{group:servers}")
                                    .build()
                            )
                            .add("serverPattern", "{status} - {serverName} - {services}")
                            .add("statuses",
                                JsonBuilder()
                                    .add("allOnline", "\uD83D\uDFE2 All servers are online!")
                                    .add("allOffline", "\uD83D\uDD34 All servers are offline!")
                                    .add("someOffline", "\uD83D\uDFE1 Some servers are offline!")
                                    .build()
                            )
                            .build()
                    )
                .build()
            )
            .add("mail",
                JsonBuilder()
                    .add("enabled", false)
                    .add("smtp", "")
                    .add("port", 465)
                    .add("username", "")
                    .add("password", "")
                    .add("address", "")
                    .add("senderName", "UptimerMailer")
                    .add("sendTo", "")
                .build()
            )
        .build()
    ),
    SERVERS(
        JsonBuilder()
            .add(
                "servers",
                JsonArrayBuilder()
                    .add(
                        JsonBuilder()
                            .add("ip", "8.8.8.8")
                            .add("serverName", "Example server")
                            .add("services", "Google DNS")
                            .add("upMessage", "Server {serverName}({ip}) is UP!  It was offline {downTime} seconds!")
                        .build()
                    )
                    .build()
            )
        .build()
    )

}