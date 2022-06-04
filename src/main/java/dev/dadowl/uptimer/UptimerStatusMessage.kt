package dev.dadowl.uptimer

import dev.dadowl.uptimer.utils.Config

class UptimerStatusMessage(private val config: Config) {

    var id: Int = -1
    var lines: ArrayList<String> = ArrayList(listOf(
        "{status}",
        "",
        "Servers:",
        "{servers}"
    ))
    var statuses = HashMap(hashMapOf(
        "allOnline" to "🟢 All servers are online!",
        "allOffline" to "🔴 All servers are offline!",
        "someOffline" to "🟡 Some servers are offline!"
    ))
    var serverPattern = "{status} - {serverName} - {services}"

    init {
        this.id = config.getInt("msgId")
        var jarray = config.getJsonArray("lines")
        if (jarray.isEmpty) {
            Uptimer.stop("No status lines found. Use default")
        } else {
            lines.clear()
            jarray.forEach{ line ->
                lines.add(line.asString)
            }
        }

        statuses.forEach{ (status, _) ->
            val cnf = Config(config.getJsonObject("statuses"))
            if (cnf.getString(status).isNotEmpty()){
                statuses[status] = cnf.getString(status)
            }
        }
        if (config.getString("serverPattern").isNotEmpty())
            serverPattern = config.getString("serverPattern")
    }
}