package xyz.kotlinw.serialization.json

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder

const val JsonClassDiscriminator = "kType"

fun standardJson(configure: JsonBuilder.() -> Unit = {}) =
    Json {
        configure()
        classDiscriminator = JsonClassDiscriminator
    }

fun standardLongTermJson(configure: JsonBuilder.() -> Unit = {}) =
    standardJson {
        configure()

        encodeDefaults = true
        ignoreUnknownKeys = true
        isLenient = false
        allowStructuredMapKeys = false
        explicitNulls = false
        coerceInputValues = false
        useArrayPolymorphism = false
        allowSpecialFloatingPointValues = true
        useAlternativeNames = true
    }
