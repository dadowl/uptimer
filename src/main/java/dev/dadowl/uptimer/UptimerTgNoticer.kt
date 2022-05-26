package dev.dadowl.uptimer

import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL

class UptimerTgNoticer(val tg_token: String, val tg_username: String, val tg_channel: Long){

    fun sendMessage(text: String){
        var urlString = "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s"

        urlString = String.format(urlString, tg_token, tg_channel, text)

        val url = URL(urlString)
        val conn = url.openConnection()

        val inputStream = BufferedInputStream(conn.getInputStream())
        val br = BufferedReader(InputStreamReader(inputStream))

        val response = br.readText()

        println(response)
    }

}