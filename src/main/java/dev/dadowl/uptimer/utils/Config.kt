package dev.dadowl.uptimer.utils

import com.google.gson.*

class Config() {

    var json = JsonObject()

    constructor(json: JsonObject): this(){
        this.json = json
    }

    private fun getVariable(path: String): JsonElement? {
        var obj = GsonBuilder().create().fromJson(json, JsonObject::class.java)
        val seg = path.split("\\.".toRegex()).toTypedArray()
        for (element in seg) {
            obj = if (obj != null) {
                val ele = obj[element] ?: return null
                if (!ele.isJsonObject) return ele else ele.asJsonObject
            } else {
                return null
            }
        }
        return obj
    }

    fun getBoolean(str: String): Boolean{
        return if (getVariable(str) != null) getVariable(str)!!.asBoolean else false
    }
    fun getBoolean(str: String, defaultValue: Boolean): Boolean{
        return if (getVariable(str) != null) getVariable(str)!!.asBoolean else defaultValue
    }
    fun getNotBoolean(str: String): Boolean{
        return !getBoolean(str)
    }

    fun getString(str: String): String{
        return if (getVariable(str) != null) getVariable(str)!!.asString else ""
    }

    fun getString(str: String, defaultValue: String): String{
        return if (getVariable(str) != null) getVariable(str)!!.asString else defaultValue
    }

    fun getLong(str: String): Long{
        return if (getVariable(str) != null) getVariable(str)!!.asLong else -1L
    }

    fun getInt(str: String): Int{
        return if (getVariable(str) != null) getVariable(str)!!.asInt else -1
    }

    fun getInt(str: String, value: Int): Int{
        return if (getVariable(str) != null) getVariable(str)!!.asInt else value
    }

    fun getJsonObject(str: String): JsonObject{
        return if (getVariable(str) != null) getVariable(str)!!.asJsonObject else JsonObject()
    }

    fun getJsonArray(str: String): JsonArray{
        return if (getVariable(str) != null) getVariable(str)!!.asJsonArray else JsonArray()
    }

}