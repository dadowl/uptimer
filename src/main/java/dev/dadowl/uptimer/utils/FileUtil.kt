package dev.dadowl.uptimer.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.nio.charset.Charset

object FileUtil {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun openFile(fileName: String, json: JsonObject): JsonObject{
        val filePath = getPath(fileName)
        val file = File(filePath)

        if (!file.exists()) {
            UptimerLogger.info("Create new file $filePath")
            file.parentFile.mkdirs()
            return try {
                file.createNewFile()
                saveFile(fileName, json)
                json
            } catch (ex: Exception){
                UptimerLogger.error("Failed to load $filePath")
                ex.printStackTrace()
                json
            }
        }

        val fis = FileInputStream(file)

        if (file.length() < 1){
            fis.close()
            return json
        }

        val data = ByteArray(file.length().toInt())
        fis.read(data)
        fis.close()

        val js = String(data, Charset.forName("UTF-8"))

        val job = gson.fromJson(js, JsonElement::class.java).asJsonObject

        if (job.size() == 0) {
            return json
        }

        UptimerLogger.info("Loaded file $filePath")
        return job
    }

    fun saveFile(fileName: String, settings: JsonObject){
        val filePath = getPath(fileName)
        val file = File(filePath)

        try {
            val writer = FileWriter(file)
            writer.write(gson.toJson(settings))
            writer.close()
            UptimerLogger.info("File saved $filePath")
        } catch (e: java.lang.Exception) {
            UptimerLogger.error("Failed to save file $filePath")
            e.printStackTrace()
        }

    }

    private fun getPath(fileName: String): String{
        return "./$fileName"
    }


}