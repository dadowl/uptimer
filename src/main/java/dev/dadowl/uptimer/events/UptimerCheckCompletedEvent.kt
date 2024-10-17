package dev.dadowl.uptimer.events

import dev.dadowl.uptimer.UptimerItem
import java.util.EventObject

class UptimerCheckCompletedEvent(source: List<UptimerItem>) : EventObject(source)