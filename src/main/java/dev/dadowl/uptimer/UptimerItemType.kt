package dev.dadowl.uptimer

import dev.dadowl.uptimer.utils.Utils

enum class UptimerItemType(val checkValid: (String) -> Boolean) {
    SITE(fun (value: String): Boolean{
        val reg = Regex(
            "((http|https)://)(www.)?[a-zA-Z0-9@:%._\\+~#?&//=-]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%._\\+~#?&//=]*)"
        )
        return reg.containsMatchIn(value)
    }),
    HOST(fun (value: String): Boolean{
        /*val reg = Regex("^[a-zA-Z0-9][a-zA-Z0-9-]{1,61}[a-zA-Z0-9](?:\\.[a-zA-Z]{2,})+\$")
        return reg.containsMatchIn(value)*/
        return true
    }),
    IP(fun (value: String): Boolean{
        if (value.split(":").size > 1){
            if (Utils.isValidIp(value.split(":")[0]))
                return true
        } else {
            if (Utils.isValidIp(value))
                return true
        }
        return false
    })
}