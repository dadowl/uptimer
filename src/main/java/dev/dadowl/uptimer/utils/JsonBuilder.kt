package dev.dadowl.uptimer.utils

import com.google.gson.JsonArray
import com.google.gson.JsonObject

class JsonBuilder {
    private var json = JsonObject()

    fun add(key: String?, value: String?): JsonBuilder {
        json.addProperty(key, value)
        return this
    }

    fun add(key: String?, value: Int): JsonBuilder {
        json.addProperty(key, value)
        return this
    }

    fun add(key: String?, value: Boolean): JsonBuilder {
        json.addProperty(key, value)
        return this
    }

    fun add(key: String?, value: Long): JsonBuilder {
        json.addProperty(key, value)
        return this
    }

    fun add(key: String?, value: JsonObject?): JsonBuilder {
        json.add(key, value)
        return this
    }

    fun add(key: Int, value: JsonObject?): JsonBuilder {
        json.add(key.toString(), value)
        return this
    }

    fun add(key: String?, array: JsonArray?): JsonBuilder {
        json.add(key, array)
        return this
    }

    override fun toString(): String {
        return json.toString()
    }

    fun build(): JsonObject {
        return json
    }
}