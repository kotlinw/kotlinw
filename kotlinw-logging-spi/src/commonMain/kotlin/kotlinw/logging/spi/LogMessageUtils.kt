package kotlinw.logging.spi

import kotlinw.logging.api.LogMessage
import kotlinw.logging.api.LogMessage.Empty
import kotlinw.logging.api.LogMessage.SimpleText
import kotlinw.logging.api.LogMessage.SimpleValue
import kotlinw.logging.api.LogMessage.Structured
import kotlinw.logging.api.LogMessage.Structured.Segment.NamedValue
import kotlinw.logging.api.LogMessage.Structured.Segment.Text
import kotlinw.logging.api.LogMessage.Structured.Segment.Value

fun LogMessage.processPlaceholders(
    placeholderProvider: (value: Any?) -> String,
    onArgument: (value: Any?) -> Unit,
    onNamedArgument: (name: String, value: Any?) -> Unit
): String =
    when (this) {
        is SimpleText -> messageText

        is SimpleValue -> {
            onArgument(value)
            placeholderProvider(value)
        }

        is Structured -> {
            val messageBuilder = StringBuilder()

            for (segment in segments) {
                when (segment) {
                    is NamedValue -> {
                        val value = segment.value
                        messageBuilder.append(placeholderProvider(value))
                        onNamedArgument(segment.name, value)
                    }

                    is Value -> {
                        val value = segment.value
                        messageBuilder.append(placeholderProvider(value))
                        onArgument(value)
                    }

                    is Text -> messageBuilder.append(segment.text)
                }
            }

            messageBuilder.toString()
        }

        Empty -> ""
    }
