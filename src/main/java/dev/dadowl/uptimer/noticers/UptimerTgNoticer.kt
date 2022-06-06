package dev.dadowl.uptimer.noticers

import dev.dadowl.uptimer.Uptimer
import dev.dadowl.uptimer.UptimerItem
import dev.dadowl.uptimer.UptimerLogger
import dev.dadowl.uptimer.UptimerStatusMessage
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

    val enabled = config.getBoolean("enabled", false)
    private val tg_token = config.getString("token")
    private val tg_username = config.getString("username")
    private val tg_channel = config.getLong("channel")
    val statusMessage = UptimerStatusMessage(Config(config.getJsonObject("status")))

    private val RECONNECT_PAUSE = 10000L

    init {
        if (enabled) {
            if (tg_token.isEmpty() || tg_username.isEmpty()){
                Uptimer.stop("Telegram settings error.")
            }

            if (tg_channel == -1L){
                UptimerLogger.warn("Telegram channel id is invalid or not found. Messages will not be sent.")
            }

            if (statusMessage.id == -1){
                UptimerLogger.warn("Status message id is -1! Ignoring this function.")
            }
        } else {
            UptimerLogger.warn("Telegram noticer is disabled.")
        }
    }

    override fun getBotToken(): String {
        return tg_token
    }

    override fun getBotUsername(): String {
        return tg_username
    }

    fun connect() {
        if (!enabled) return
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
        if (!enabled || tg_channel == -1L) return
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
        if (!enabled || statusMessage.id == -1 || tg_channel == -1L) return

        var status: String = if (Uptimer.uptimerItems.filter { it.status == UptimerItem.PingStatus.ONLINE }.size == Uptimer.uptimerItems.size){
            statusMessage.statuses["allOnline"]!!
        } else if (Uptimer.uptimerItems.filter { it.status == UptimerItem.PingStatus.OFFLINE }.size == Uptimer.uptimerItems.size){
            statusMessage.statuses["allOffline"]!!
        } else {
            statusMessage.statuses["someOffline"]!!
        }

        val lines = ArrayList<String>()
        for (line in statusMessage.lines) {
            var l = line

            if (l.contains("{status}")){
                l = l.replace(l, status)
            }
            /*if (l.contains("{servers}")){
                var serversString = StringBuilder()
                Uptimer.uptimerItems.filter { it.group == "servers" }.forEach { server ->
                    serversString.append(UptimerItem.getMessage(statusMessage.serverPattern, server) + "\n")
                }
                l = l.replace(l, serversString.toString())
            }*/
            if (l.contains("group")){
                var currentGroup = l.split(":")[1]
                currentGroup = currentGroup.substring(0, currentGroup.length - 1)

                val groupServers = Uptimer.uptimerItems.filter { it.group == currentGroup }
                if (groupServers.isNotEmpty()){
                    var serversString = StringBuilder()
                    var i = 0
                    groupServers.forEach { server ->
                        i++
                        serversString.append(UptimerItem.getMessage(statusMessage.serverPattern, server) + if (i != groupServers.size) "\n" else "")
                    }
                    l = l.replace(l, serversString.toString())
                }
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