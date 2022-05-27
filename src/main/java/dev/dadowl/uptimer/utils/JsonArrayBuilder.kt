package dev.dadowl.uptimer.utils

import com.google.gson.JsonArray
import com.google.gson.JsonObject

class JsonArrayBuilder {
    private var json = JsonArray()

    fun add(value: String?): JsonArrayBuilder {
        json.add(value)
        return this
    }

    fun add(value: Int): JsonArrayBuilder {
        json.add(value)
        return this
    }

    fun add(value: Boolean): JsonArrayBuilder {
        json.add(value)
        return this
    }

    fun add(value: Long): JsonArrayBuilder {
        json.add(value)
        return this
    }

    fun add(value: JsonObject): JsonArrayBuilder {
        json.add(value)
        return this
    }

    fun add(value: JsonArray): JsonArrayBuilder {
        json.add(value)
        return this
    }

    override fun toString(): String {
        return json.toString()
    }

    fun build(): JsonArray {
        return json
    }
}