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

    fun formatDuration(seconds: Long): String {
        if (seconds <= 0) return "0s"

        val years = seconds / (365 * 24 * 60 * 60)
        val remainingAfterYears = seconds % (365 * 24 * 60 * 60)
        val months = remainingAfterYears / (30 * 24 * 60 * 60)
        val remainingAfterMonths = remainingAfterYears % (30 * 24 * 60 * 60)
        val days = remainingAfterMonths / (24 * 60 * 60)
        val remainingAfterDays = remainingAfterMonths % (24 * 60 * 60)
        val hours = remainingAfterDays / (60 * 60)
        val remainingAfterHours = remainingAfterDays % (60 * 60)
        val minutes = remainingAfterHours / 60
        val remainingSeconds = remainingAfterHours % 60

        val parts = mutableListOf<String>()
        if (years > 0) parts.add("${years}y")
        if (months > 0) parts.add("${months}m")
        if (days > 0) parts.add("${days}d")
        if (hours > 0) parts.add("${hours}h")
        if (minutes > 0) parts.add("${minutes}m")
        if (remainingSeconds > 0 && parts.isEmpty()) parts.add("${remainingSeconds}s")

        return if (parts.isEmpty()) "0s" else parts.joinToString(" ")
    }

}