package dev.dadowl.uptimer.models

import dev.dadowl.uptimer.utils.Utils

enum class UptimerItemType(val isValid: (String) -> Boolean) {
    SITE(fun(value: String): Boolean {
        val reg = Regex(
            "((http|https)://)(www.)?[a-zA-Z0-9@:%._\\+~#?&//=-]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%._\\+~#?&//=]*)"
        )
        return reg.containsMatchIn(value)
    }),
    HOST(fun(_: String): Boolean {
        return true
    }),
    IP(fun(value: String): Boolean {
        if (value.split(":").size > 1) {
            val parts = value.split(":")
            val ip = parts[0]
            val port = parts[1]
            if (Utils.isValidIp(ip) && port.toIntOrNull() != null && port.toInt() in 0..65535) {
                return true
            }
        } else {
            if (Utils.isValidIp(value)) {
                return true
            }
        }
        return false
    })
}