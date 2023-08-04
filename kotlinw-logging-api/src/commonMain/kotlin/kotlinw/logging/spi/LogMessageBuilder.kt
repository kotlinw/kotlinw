package kotlinw.logging.spi

import arrow.core.NonFatal
import kotlinw.logging.api.LogMessage
import kotlinw.logging.api.LogMessage.FailedEvaluationPlaceholder
import kotlinw.logging.api.LogMessage.Structured.Segment.NamedValue
import kotlinw.logging.api.LogMessage.Structured.Segment.Value

typealias LogMessageProvider = LogMessageBuilder.() -> Any?

sealed interface LogMessageBuilder {

    fun arg(value: Any?): Value = Value(value)

    fun arg(valueProvider: () -> Any?): Value

    fun named(name: String, value: Any?): NamedValue = NamedValue(name, value)

    fun named(name: String, valueProvider: () -> Any?): NamedValue

    operator fun String.div(value: Value): LogMessageBuilder

    operator fun String.div(value: NamedValue): LogMessageBuilder

    operator fun String.div(value: Any?): LogMessageBuilder

    operator fun String.div(values: List<Any?>): LogMessageBuilder

    operator fun String.div(namedValues: Map<String, Any?>): LogMessageBuilder

    operator fun String.div(valueProvider: () -> Any?): LogMessageBuilder

    operator fun div(text: String): LogMessageBuilder

    operator fun div(value: Value): LogMessageBuilder

    operator fun div(namedValue: NamedValue): LogMessageBuilder

    operator fun div(argument: Any?): LogMessageBuilder

    operator fun div(values: List<Any?>): LogMessageBuilder

    operator fun div(namedValues: Map<String, Any?>): LogMessageBuilder

    operator fun div(valueProvider: () -> Any?): LogMessageBuilder
}

private fun safeToString(value: Any?) =
    try {
        value.toString()
    } catch (e: Throwable) {
        if (NonFatal(e)) {
            FailedEvaluationPlaceholder(e).toString()
        } else {
            throw e
        }
    }

internal class LogMessageBuilderImpl : LogMessageBuilder {

    private val messageSegments = mutableListOf<LogMessage.Structured.Segment>()

    override fun arg(valueProvider: () -> Any?): Value =
        Value(
            try {
                valueProvider()
            } catch (e: Throwable) {
                FailedEvaluationPlaceholder(e)
            }
        )

    override fun named(name: String, valueProvider: () -> Any?): NamedValue =
        NamedValue(
            name,
            try {
                valueProvider()
            } catch (e: Throwable) {
                FailedEvaluationPlaceholder(e)
            }
        )

    override fun String.div(value: Value): LogMessageBuilder {
        addText(this)
        addInlineArgument(value)
        return this@LogMessageBuilderImpl
    }

    override fun String.div(value: NamedValue): LogMessageBuilder {
        addText(this)
        addNamedArgument(value.name, value.value)
        return this@LogMessageBuilderImpl
    }

    override operator fun String.div(value: Any?): LogMessageBuilder = div(arg(value))

    override operator fun String.div(values: List<Any?>): LogMessageBuilder {
        addText(this)
        addArguments(values)
        return this@LogMessageBuilderImpl
    }

    override operator fun String.div(namedValues: Map<String, Any?>): LogMessageBuilder {
        addText(this)
        addNamedArguments(namedValues)
        return this@LogMessageBuilderImpl
    }

    override operator fun String.div(valueProvider: () -> Any?): LogMessageBuilder {
        addText(this)
        addInlineArgument(valueProvider)
        return this@LogMessageBuilderImpl
    }

    override operator fun div(text: String): LogMessageBuilder {
        addTextOrArgument(text)
        return this
    }

    override operator fun div(value: Value): LogMessageBuilder {
        addInlineArgument(value)
        return this
    }

    override operator fun div(namedValue: NamedValue): LogMessageBuilder {
        addNamedArgument(namedValue)
        return this
    }

    override fun div(argument: Any?): LogMessageBuilder {
        addInlineArgument(Value(argument))
        return this
    }

    override fun div(values: List<Any?>): LogMessageBuilder {
        addArguments(values)
        return this
    }

    override fun div(namedValues: Map<String, Any?>): LogMessageBuilder {
        addNamedArguments(namedValues)
        return this
    }

    override operator fun div(valueProvider: () -> Any?): LogMessageBuilder {
        addInlineArgument(valueProvider)
        return this
    }

    private fun addArguments(values: List<Any?>) {
        val lastIndex = values.lastIndex
        values.forEachIndexed { index, value ->
            addInlineArgument(Value(value))

            if (index < lastIndex) {
                addText(", ")
            }
        }
    }

    private fun addNamedArguments(namedValues: Map<String, Any?>) {
        val lastIndex = namedValues.size - 1
        namedValues.entries.forEachIndexed { index, (name, value) ->
            addText("$name=")
            addNamedArgument(name, value)

            if (index < lastIndex) {
                addText(", ")
            }
        }
    }

    private fun addText(text: String) {
        messageSegments.add(LogMessage.Structured.Segment.Text(text))
    }

    private fun addTextOrArgument(value: String) {
        messageSegments.add(
            if (messageSegments.isEmpty() || messageSegments.last() !is LogMessage.Structured.Segment.Text)
                LogMessage.Structured.Segment.Text(value)
            else
                Value(value)
        )
    }

    private fun addNamedArgument(namedValue: NamedValue) {
        messageSegments.add(namedValue)
    }

    private fun addNamedArgument(argumentName: String, argumentValue: Any?) {
        addNamedArgument(NamedValue(argumentName, argumentValue))
    }

    private fun addNamedArgument(argumentName: String, argumentValueProvider: () -> Any?) {
        addNamedArgument(
            argumentName,
            try {
                argumentValueProvider()
            } catch (e: Throwable) {
                FailedEvaluationPlaceholder(e)
            }
        )
    }

    private fun addInlineArgument(argumentValue: Value) {
        messageSegments.add(argumentValue)
    }

    private fun addInlineArgument(argumentProvider: () -> Any?) {
        addInlineArgument(
            Value(
                try {
                    argumentProvider()
                } catch (e: Throwable) {
                    FailedEvaluationPlaceholder(e)
                }
            )
        )
    }

    internal fun build() =
        if (messageSegments.isEmpty()) LogMessage.Empty else LogMessage.Structured(messageSegments)
}

internal fun buildLogMessage(logMessageProvider: LogMessageProvider): LogMessage =
    try {
        val logMessageBuilder = LogMessageBuilderImpl()
        when (val logMessage = logMessageBuilder.logMessageProvider()) {
            is String -> LogMessage.SimpleText(logMessage)
            is LogMessageBuilderImpl -> logMessage.build()
            Unit -> LogMessage.Empty
            else -> LogMessage.SimpleValue(logMessage)
        }
    } catch (e: Exception) {
        LogMessage.SimpleText(FailedEvaluationPlaceholder(e).toString())
    }
