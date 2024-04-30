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
    positionalArgumentPlaceholderProvider: (value: Any?) -> String,
    namedArgumentPlaceholderProvider: (name: String, value: Any?) -> String,
    onArgument: (value: Any?) -> Unit,
    onNamedArgument: (name: String, value: Any?) -> Unit
): String =
    when (this) {
        is SimpleText -> messageText

        is SimpleValue -> {
            onArgument(value)
            positionalArgumentPlaceholderProvider(value)
        }

        is Structured -> {
            val messageBuilder = StringBuilder()

            for (segment in segments) {
                when (segment) {
                    is NamedValue -> {
                        val name = segment.name
                        val value = segment.value
                        messageBuilder.append(namedArgumentPlaceholderProvider(name, value))
                        onNamedArgument(name, value)
                    }

                    is Value -> {
                        val value = segment.value
                        messageBuilder.append(positionalArgumentPlaceholderProvider(value)) // TODO ezeknek is lehetne egy generált nevet adni
                        onArgument(value)
                    }

                    is Text -> messageBuilder.append(segment.text)
                }
            }

            messageBuilder.toString()
        }

        Empty -> ""
    }
