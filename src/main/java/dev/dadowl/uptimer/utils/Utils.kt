package dev.dadowl.uptimer.utils

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

    fun replacePlaceholders(str: String, map: HashMap<String, String>): String {
        var newStr = str

        map.forEach { (key, value) ->
            if (newStr.contains(key)){
                newStr = newStr.replace(key, value)
            }
        }

        return newStr
    }

}