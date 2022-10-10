package dev.dadowl.uptimer.utils

import com.google.gson.JsonObject
import dev.dadowl.uptimer.Uptimer
import org.apache.commons.validator.routines.InetAddressValidator
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Utils {

    var validator: InetAddressValidator = InetAddressValidator.getInstance()
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss")

    fun isValidIp(ip: String): Boolean{
        if (validator.isValidInet4Address(ip)) {
            return true
        }
        return false
    }

    fun isValidIpV6(ip: String): Boolean{
        if (validator.isValidInet6Address(ip)) {
            return true
        }
        return false
    }

    fun isValidURL(url: String): Boolean {
        return try {
            URL(url).toURI()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getOnlyTime(date: LocalDateTime): String{
        return date.format(timeFormat)
    }

    fun lastChar(str: String): String{
        return str.toCharArray()[str.length - 1].toString()
    }

}