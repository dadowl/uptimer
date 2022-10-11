package dev.dadowl.uptimer.noticers

import dev.dadowl.uptimer.UptimerItem
import dev.dadowl.uptimer.UptimerLogger
import dev.dadowl.uptimer.events.UptimerEventListener
import dev.dadowl.uptimer.events.UptimerEventType
import dev.dadowl.uptimer.events.UptimerPingEvent
import dev.dadowl.uptimer.utils.Config
import java.util.*
import javax.mail.Message
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage


class UptimerMailNoticer(config: Config) : UptimerEventListener {

    private var enabled = config.getBoolean("enabled", false)
    private val host = config.getString("smtp")
    private val port = config.getInt("port")
    private val username = config.getString("username")
    private val password = config.getString("password")
    private val senderName = config.getString("senderName")
    private val address = config.getString("address")
    private val sendTo = config.getString("sendTo")

    init {
        if (enabled){
            if (host.isEmpty() || port == 0 || username.isEmpty() || password.isEmpty() || senderName.isEmpty() || address.isEmpty() || sendTo.isEmpty()){
                enabled = false
                UptimerLogger.warn("Mail noticer is disabled. Settings error...")
            }
        } else {
            UptimerLogger.warn("Mail noticer is disabled.")
        }
    }

    private fun sendLetter(subject: String, text: String){
        if (!enabled) return

        val props = System.getProperties()

        props["mail.smtp.host"] = host
        props["mail.smtp.ssl.enable"] = "true"
        props["mail.smtp.auth"] = "true"

        val session = Session.getDefaultInstance(props, null)


        val message = MimeMessage(session)

        message.subject = subject
        message.addHeader("Content-type", "text/HTML; charset=UTF-8")
        message.addHeader("format", "flowed")
        message.addHeader("Content-Transfer-Encoding", "8bit")
        message.setFrom(InternetAddress(address, senderName))
        message.setText(text, "UTF-8")
        message.addRecipient(Message.RecipientType.TO, InternetAddress(sendTo))
        message.sentDate = Date()

        val transport = session.transport
        transport.connect(host, port, username, password)

        transport.sendMessage(message, message.allRecipients)

    }

    override fun onPingEvent(event: UptimerPingEvent) {
        val uptimerItem = event.source as UptimerItem
        when(event.eventType){
            UptimerEventType.PING_ONLINE -> {
                sendLetter("${uptimerItem.value} is UP", UptimerItem.getMessage(uptimerItem.upMsg, uptimerItem))
            }
            UptimerEventType.PING_OFFLINE -> {
                sendLetter("${uptimerItem.value} is DOWN", UptimerItem.getMessage(uptimerItem.downMsg, uptimerItem))
            }
            else -> {}
        }
    }
}