package dev.dadowl.uptimer.utils

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import dev.dadowl.uptimer.UptimerLogger

class Config(val json: JsonObject) {

    private fun getVariable(varName: String): JsonElement? {
        return try {
            json.get(varName)
        } catch(ex: Exception){
            UptimerLogger.error("Variable $varName from json object was not found")
            null
        }
    }

    fun getBoolean(str: String): Boolean{
        return if (getVariable(str) != null) getVariable(str)!!.asBoolean else false
    }
    fun getNotBoolean(str: String): Boolean{
        return !getBoolean(str)
    }

    fun getString(str: String): String{
        return if (getVariable(str) != null) getVariable(str)!!.asString else ""
    }

    fun getLong(str: String): Long{
        return if (getVariable(str) != null) getVariable(str)!!.asLong else 0
    }

    fun getInt(str: String): Int{
        return if (getVariable(str) != null) getVariable(str)!!.asInt else 0
    }

    fun getJsonObject(str: String): JsonObject{
        return if (getVariable(str) != null) getVariable(str)!!.asJsonObject else JsonObject()
    }

    fun getJsonArray(str: String): JsonArray{
        return if (getVariable(str) != null) getVariable(str)!!.asJsonArray else JsonArray()
    }

}