package dev.dadowl.uptimer.utils

import org.apache.commons.validator.routines.InetAddressValidator

object Utils {

    var validator: InetAddressValidator = InetAddressValidator.getInstance()

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

}