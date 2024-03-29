package kotlinw.logging.spi

import kotlinw.logging.api.LogMessage.SimpleText
import kotlinw.logging.api.LogMessage.Structured
import kotlinw.logging.api.LogMessage.Structured.Segment.NamedValue
import kotlinw.logging.api.LogMessage.Structured.Segment.Text
import kotlinw.logging.api.LogMessage.Structured.Segment.Value
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinw.logging.api.LogMessage
import kotlinw.logging.api.LogMessage.SimpleValue

class LogMessageBuilderImplTest {

    @Test
    fun testNoMessage() {
        assertEquals(LogMessage.Empty, buildLogMessage { })
    }

    @Test
    fun testMessageOnly() {
        assertEquals(SimpleText("Only message."), buildLogMessage { "Only message." })
    }

    @Test
    fun testSimpleArgumentOnly() {
        assertEquals(SimpleValue(5), buildLogMessage { 5 })
        assertEquals(SimpleValue(1.1), buildLogMessage { 1.1 })
    }

    @Test
    fun testMessageWithOneArgument() {
        assertEquals(
            Structured(Text("String:"), Value(5)),
            buildLogMessage { "String:" / arg(5) }
        )
        assertEquals(
            Structured(Text("Number:"), Value(5)),
            buildLogMessage { "Number:" / 5 }
        )
        assertEquals(
            Structured(Text("String:"), Value("arg")),
            buildLogMessage { "String:" / arg("arg") }
        )
        assertEquals(
            Structured(Text("String:"), Value("arg")),
            buildLogMessage { "String:" / "arg" }
        )
    }

    @Test
    fun testMessageWithMultipleArguments() {
        assertEquals(
            Structured(
                Text("Number: "),
                Value(5),
                Text(" String: "),
                Value("text")
            ),
            buildLogMessage { "Number: " / 5 / " String: " / "text" }
        )
        assertEquals(
            Structured(
                Text("Int: "),
                Value(5),
                Text(", String: "),
                Value("text"),
                Text(", Double: "),
                Value(1.1)
            ),
            buildLogMessage { "Int: " / 5 / ", String: " / "text" / ", Double: " / 1.1 }
        )
    }

    @Test
    fun testMessageWithOneNamedArgument() {
        assertEquals(
            Structured(Text("Number:"), NamedValue("number", 5)),
            buildLogMessage { "Number:" / named("number", 5) }
        )
    }

    @Test
    fun testMessageWithNamedArgumentsAsMap() {
        assertEquals(
            Structured(Text("From map:"), NamedValue("number", 5), Text(", "), NamedValue("string", "text")),
            buildLogMessage { "From map:" / mapOf("number" to 5, "string" to "text") }
        )
    }
}
