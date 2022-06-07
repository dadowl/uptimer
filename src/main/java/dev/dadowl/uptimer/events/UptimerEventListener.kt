package dev.dadowl.uptimer.events

import java.util.*

interface UptimerEventListener : EventListener {
    fun onPingEvent(event: UptimerPingEvent)
}