package kotlinw.logging.spi

import kotlinw.logging.api.LogMessage
import kotlinw.logging.api.LogMessage.SimpleText
import kotlinw.logging.api.LogMessage.Structured
import kotlinw.logging.api.LogMessage.Structured.Segment.NamedValue
import kotlinw.logging.api.LogMessage.Structured.Segment.Text
import kotlinw.logging.api.LogMessage.Structured.Segment.Value
import kotlin.test.Test
import kotlin.test.assertEquals

class LogMessageBuilderImplTest {

    @Test
    fun testNoMessage() {
        assertEquals(SimpleText(""), buildLogMessage { })
    }

    @Test
    fun testMessageOnly() {
        assertEquals(SimpleText("Only message."), buildLogMessage { "Only message." })
    }

    @Test
    fun testMessageWithOnePositionalArgument() {
        assertEquals(
            Structured(Text("Number:"), Value(5)),
            buildLogMessage { "Number:" / 5 }
        )
        assertEquals(
            Structured(Text("String:"), Value("arg")),
            buildLogMessage { "String:" / "arg" }
        )
    }

    @Test
    fun testMessageWithMultiplePositionalArguments() {
        assertEquals(
            Structured(
                Text("Number: "),
                Value(5),
                Text(" String: "),
                Value("text")
            ),
            buildLogMessage { "Number: " / 5 / " String: " / "text" }
        )
    }

    @Test
    fun testMessageWithOneNamedArgument() {
        assertEquals(
            Structured(Text("Number:"), NamedValue("number", 5)),
            buildLogMessage { "Number:" / ("number" to 5) }
        )
    }
}
