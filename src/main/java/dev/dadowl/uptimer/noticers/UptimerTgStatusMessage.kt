package dev.dadowl.uptimer.noticers

import dev.dadowl.uptimer.Uptimer
import dev.dadowl.uptimer.UptimerItem
import dev.dadowl.uptimer.UptimerLogger
import dev.dadowl.uptimer.utils.Config
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText

class UptimerTgStatusMessage(
    private val config: Config,
    private val tgChannel: Long,
    private val tg: UptimerTgNoticer
) {

    var id: Int = -1
    private var lines: ArrayList<String> = arrayListOf(
        "{status}",
        "",
        "Servers:",
        "{group:servers}"
    )
    private var statuses = hashMapOf(
        "allOnline" to "🟢 All servers are online!",
        "allOffline" to "🔴 All servers are offline!",
        "someOffline" to "🟡 Some servers are offline!"
    )
    private var serverPattern = "{status} - {serverName} - {services}"
    private var statusText = ""

    init {
        if (id == -1){
            UptimerLogger.warn("Status message id is -1! Ignoring this function.")
        }

        this.id = config.getInt("msgId", -1)

        val jarray = config.getJsonArray("lines")
        if (jarray.isEmpty) {
            UptimerLogger.warn("No status lines found. Use default lines.")
        } else {
            lines.clear()
            jarray.forEach { line ->
                lines.add(line.asString)
            }
        }

        statuses.forEach { (status, _) ->
            val cnf = Config(config.getJsonObject("statuses"))
            if (cnf.getString(status).isNotEmpty()) {
                statuses[status] = cnf.getString(status)
            }
        }
        if (config.getString("serverPattern").isNotEmpty())
            serverPattern = config.getString("serverPattern")
    }

    fun updateStatusMessage(uptimerItems: List<UptimerItem>) {
        if (id == -1 || tgChannel == -1L) return

        val status = statuses[Uptimer.getItemsStatus()] ?: "allOffline"

        var finalString = ""
        for (line in lines) {
            var currentLine = line

            if (currentLine.contains("{status}")) {
                currentLine = currentLine.replace("{status}", status)
            }
            if (currentLine.contains("group")) {
                var currentGroup = currentLine.split(":")[1]
                currentGroup = currentGroup.substring(0, currentGroup.length - 1)

                val groupServers = uptimerItems.filter { it.group == currentGroup }
                if (groupServers.isNotEmpty()) {
                    val serversString = StringBuilder()
                    groupServers.forEachIndexed { index, server ->
                        serversString.append(
                            server.formatMessage(
                                serverPattern
                            ) + if (index + 1 != groupServers.size) "\n" else ""
                        )
                    }
                    currentLine = currentLine.replace(currentLine, serversString.toString())
                }
            }

            finalString += "$currentLine\n"
        }

        if (statusText != finalString) {
            statusText = finalString
            val edit = EditMessageText()
            edit.chatId = tgChannel.toString()
            edit.messageId = id
            edit.text = finalString

            try {
                tg.execute(edit)
            } catch (_: Exception){}
        }
    }
}