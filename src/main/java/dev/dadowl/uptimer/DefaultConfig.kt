package dev.dadowl.uptimer

import com.google.gson.JsonObject
import dev.dadowl.uptimer.utils.JsonArrayBuilder
import dev.dadowl.uptimer.utils.JsonBuilder

enum class DefaultConfig(val json: JsonObject) {

    DEFAULT(
        JsonBuilder()
            .add("pingEvery", 5)
            .add("downTryes", 3)
            .add("upMessage", "Server {serverName}({ip}) is UP!")
            .add("downMessage", "Server {serverName}({ip}) is DOWN!")
        .build()
    ),
    TELEGRAM(
        JsonBuilder()
            .add(
                "Telegram",
                JsonBuilder()
                    .add("token", "")
                    .add("username", "")
                    .add("channel", -1)
                .build()
            )
            .add(
                "status",
                JsonBuilder()
                    .add("msgId", -1)
                    .add(
                        "lines",
                        JsonArrayBuilder()
                            .add("{status}")
                            .add("")
                            .add("Servers:")
                            .add("{servers}")
                        .build()
                    )
                    .add("serverPattern", "{status} - {serverName} - {services}")
                    .add(
                        "statuses",
                        JsonBuilder()
                            .add("allOnline", "\uD83D\uDFE2 All servers are online!")
                            .add("allOffline", "\uD83D\uDD34 All servers are offline!")
                            .add("someOffline", "\uD83D\uDFE1 Some servers are offline!")
                            .build()
                    )
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