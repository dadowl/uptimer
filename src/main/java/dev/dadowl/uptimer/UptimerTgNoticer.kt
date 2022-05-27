package dev.dadowl.uptimer

import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.PinChatMessage
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession


class UptimerTgNoticer(
    private val tg_token: String,
    private val tg_username: String,
    private val tg_channel: Long,
    private val tg_statusMsgId: Int
): TelegramLongPollingBot(){

    val RECONNECT_PAUSE = 10000L

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
        val msg = SendMessage()
        msg.chatId = tg_channel.toString()
        msg.text = text
        try {
            val send = execute(msg)
            if (dev){
                UptimerLogger.info("Status message id is ${send.messageId}")
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

}