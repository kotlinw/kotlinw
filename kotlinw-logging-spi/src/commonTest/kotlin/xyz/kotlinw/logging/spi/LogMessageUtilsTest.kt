package xyz.kotlinw.logging.spi

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlinw.logging.spi.buildLogMessage
import kotlinw.logging.spi.processPlaceholders

class LogMessageUtilsTest {

    @Test
    fun testProcessPlaceholders_1() {
        val namedArguments = mutableMapOf<String, Any?>()
        assertEquals(
            "Number:{}",
            buildLogMessage { "Number:" / named("number", 5) }
                .processPlaceholders(
                    { "{}" },
                    { fail() },
                    { name, value -> namedArguments[name] = value}
                )
        )
        assertEquals(mapOf<String, Any?>("number" to 5), namedArguments)
    }

    @Test
    fun testProcessPlaceholders_2() {
        val namedArguments = mutableMapOf<String, Any?>()
        assertEquals(
            "From map: {}, {}",
            buildLogMessage { "From map: " / mapOf("number" to 5, "string" to "text") }
                .processPlaceholders(
                    { "{}" },
                    { fail() },
                    { name, value -> namedArguments[name] = value}
                )
        )
        assertEquals(mapOf<String, Any?>("number" to 5, "string" to "text"), namedArguments)
    }
}
