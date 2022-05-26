package dev.dadowl.uptimer.utils

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

object UptimerLogger {
    private val logger = LogManager.getLogger(Logger::class.java)

    fun info(str: String?) {
        logger.info(str)
    }

    fun warn(str: String?) {
        logger.warn(str)
    }

    fun error(str: String?) {
        logger.error(str)
    }
    fun error(str: String?, throwable: Throwable) {
        logger.error(str, throwable)
    }

    fun debug(str: String?) {
        logger.debug(str)
    }
}