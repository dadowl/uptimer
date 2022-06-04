package dev.dadowl.uptimer

import dev.dadowl.uptimer.utils.Config
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.PinChatMessage
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession


class UptimerTgNoticer(config: Config): TelegramLongPollingBot(){

    private val tg_token = config.getString("Telegram.token")
    private val tg_username = config.getString("Telegram.username")
    private val tg_channel = config.getLong("Telegram.channel")
    val statusMessage = UptimerStatusMessage(Config(config.getJsonObject("status")))

    private val RECONNECT_PAUSE = 10000L

    init {
        if (tg_token.isEmpty() || tg_username.isEmpty()){
            Uptimer.stop("Telegram settings error.")
        }

        if (tg_channel == -1L){
            UptimerLogger.warn("Telegram channel id is invalid or not found. Messages will not be sent.")
        }

        if (statusMessage.id == -1){
            UptimerLogger.warn("Status message id is -1! Ignoring this function.")
        }
    }

    override fun getBotToken(): String {
        return tg_token
    }

    override fun getBotUsername(): String {
        return tg_username
    }

    fun connect() {
        val telegramBotsApi = TelegramBotsApi(DefaultBotSession::class.java)
        try {
            telegramBotsApi.registerBot(this)
            UptimerLogger.info("TelegramAPI started. ")
        } catch (e: TelegramApiRequestException) {
            UptimerLogger.error("Cant Connect. Pause " + (RECONNECT_PAUSE / 1000).toString() + "sec and try again. Error: " + e.message)
            try {
                Thread.sleep(RECONNECT_PAUSE)
            } catch (e1: InterruptedException) {
                e1.printStackTrace()
                return
            }
        }
    }

    fun sendMessage(text: String, dev: Boolean){
        if (tg_channel == -1L) return
        val msg = SendMessage()
        msg.chatId = tg_channel.toString()
        msg.text = text
        try {
            val send = execute(msg)
            if (dev){
                UptimerLogger.info("Status message id is ${send.messageId}")
                Uptimer.saveStatusId(send.messageId)
                UptimerLogger.info("Status message id saved in config file.")
                val pin = PinChatMessage(tg_channel.toString(), send.messageId, false)
                execute(pin)
            }
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }

    fun sendMessage(text: String){
        return sendMessage(text, false)
    }

    override fun onUpdateReceived(update: Update?) {}

    fun updateStatusMessage(){
        if (statusMessage.id == -1 || tg_channel == -1L) return

        var status: String = if (Uptimer.uptimerItems.filter { it.status == UptimerItem.PingStatus.ONLINE }.size == Uptimer.uptimerItems.size){
            statusMessage.statuses["allOnline"]!!
        } else if (Uptimer.uptimerItems.filter { it.status == UptimerItem.PingStatus.OFFLINE }.size == Uptimer.uptimerItems.size){
            statusMessage.statuses["allOffline"]!!
        } else {
            statusMessage.statuses["someOffline"]!!
        }

        val servers = ArrayList<String>()
        Uptimer.uptimerItems.forEach { server ->
            var pattern = statusMessage.serverPattern

            if (pattern.contains("{status}")){
                pattern = pattern.replace("{status}", server.status.icon)
            }
            if (pattern.contains("{serverName}")){
                pattern = pattern.replace("{serverName}", server.serverName)
            }
            if (pattern.contains("{services}")){
                pattern = pattern.replace("{services}", server.services)
            }

            servers.add(pattern)
        }
        var serversString = ""
        servers.forEach { serversString += it + "\n" }

        val lines = ArrayList<String>()
        for (line in statusMessage.lines) {
            var l = line

            if (l.contains("{status}")){
                l = l.replace("{status}", status)
            }
            if (l.contains("{servers}")){
                l = l.replace("{servers}", serversString)
            }

            lines.add(l)
        }

        var finalString = ""
        lines.forEach { finalString += it + "\n" }

        val edit = EditMessageText()
        edit.chatId = tg_channel.toString()
        edit.messageId = statusMessage.id
        edit.text = finalString
        execute(edit)
    }
}