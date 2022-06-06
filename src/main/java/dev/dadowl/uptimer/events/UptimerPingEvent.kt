package dev.dadowl.uptimer.events

import dev.dadowl.uptimer.UptimerItem
import java.util.*

class UptimerPingEvent(source: UptimerItem, var eventType: UptimerEventType = UptimerEventType.PING_ONLINE) : EventObject(source)