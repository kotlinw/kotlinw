package kotlinw.logging.spi

import kotlinw.logging.api.LogMessage

fun LogMessage.processPlaceholders(
    placeholderProvider: (value: Any?) -> String,
    onArgument: (value: Any?) -> Unit,
    onNamedArgument: (name: String, value: Any?) -> Unit
) =
    when (this) {
        is LogMessage.SimpleText -> messageText

        is LogMessage.Structured -> {
            val messageBuilder = StringBuilder()

            for (segment in segments) {
                when (segment) {
                    is LogMessage.Structured.Segment.NamedValue -> {
                        val value = segment.value
                        messageBuilder.append(placeholderProvider(value))
                        onNamedArgument(segment.name, value)
                    }

                    is LogMessage.Structured.Segment.Value -> {
                        val value = segment.value
                        messageBuilder.append(placeholderProvider(value))
                        onArgument(value)
                    }

                    is LogMessage.Structured.Segment.Text -> messageBuilder.append(segment.text)
                }
            }

            messageBuilder.toString()
        }
    }
