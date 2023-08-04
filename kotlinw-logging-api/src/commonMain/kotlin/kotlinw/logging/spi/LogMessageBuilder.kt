package kotlinw.logging.spi

import arrow.core.NonFatal
import kotlinw.logging.api.LogMessage
import kotlinw.logging.api.LogMessage.FailedEvaluationPlaceholder

typealias LogMessageProvider = LogMessageBuilder.() -> Any?

sealed interface LogMessageBuilder {

    operator fun div(text: String): LogMessageBuilder

    operator fun String.div(argument: Any?): LogMessageBuilder

    operator fun String.div(argumentProvider: () -> Any?): LogMessageBuilder

    operator fun String.div(namedArgumentProvider: Pair<String, () -> Any?>): LogMessageBuilder

    operator fun div(argument: Any?): LogMessageBuilder

    operator fun div(argumentProvider: () -> Any?): LogMessageBuilder

    operator fun div(namedArgument: Pair<String, Any?>): LogMessageBuilder

    fun named(name: String, value: Any?) = name to value

    operator fun div(namedArguments: Map<String, Any?>): LogMessageBuilder

    operator fun div(namedArguments: List<Pair<String, Any?>>): LogMessageBuilder = div(namedArguments.toMap())
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

    private fun text(text: String): LogMessageBuilder {
        messageSegments.add(
            if (messageSegments.isEmpty() || messageSegments.last() !is LogMessage.Structured.Segment.Text)
                LogMessage.Structured.Segment.Text(text)
            else
                LogMessage.Structured.Segment.Value(text)
        )
        return this
    }

    private fun addNamedArgument(argumentName: String, argumentValue: Any?) {
        messageSegments.add(LogMessage.Structured.Segment.NamedValue(argumentName, argumentValue))
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

    private fun addInlineArgument(argumentValue: Any?) {
        if (argumentValue is String) {
            text(argumentValue)
        } else {
            messageSegments.add(LogMessage.Structured.Segment.Value(argumentValue))
        }
    }

    private fun addInlineArgument(argumentProvider: () -> Any?) {
        addInlineArgument(
            try {
                argumentProvider()
            } catch (e: Throwable) {
                FailedEvaluationPlaceholder(e)
            }
        )
    }

    override operator fun div(text: String): LogMessageBuilder {
        return text(text)
    }

    override operator fun div(argument: Any?): LogMessageBuilder {
        addInlineArgument(argument)
        return this
    }

    override operator fun String.div(argument: Any?): LogMessageBuilder {
        text(this)
        // TODO szétszedni két overloaded metódusra
        if (argument is Pair<*, *>) {
            addNamedArgument(safeToString(argument.first), argument.second)
        } else {
            addInlineArgument(argument)
        }
        return this@LogMessageBuilderImpl
    }

    override operator fun div(argumentProvider: () -> Any?): LogMessageBuilder {
        addInlineArgument(argumentProvider)
        return this
    }

    override operator fun String.div(argumentProvider: () -> Any?): LogMessageBuilder {
        text(this)
        addInlineArgument(argumentProvider)
        return this@LogMessageBuilderImpl
    }

    override fun div(namedArgument: Pair<String, Any?>): LogMessageBuilder {
        addNamedArgument(namedArgument.first, namedArgument.second)
        return this@LogMessageBuilderImpl
    }

    override fun div(namedArguments: List<Pair<String, Any?>>): LogMessageBuilder {
        namedArguments.forEachIndexed { index, namedArgument ->
            addNamedArgument(namedArgument.first, namedArgument.second)

            if (index < namedArguments.lastIndex) {
                text(", ")
            }
        }

        return this
    }

    override fun div(namedArguments: Map<String, Any?>): LogMessageBuilder =
        div(namedArguments.map { it.key to it.value })

    override fun String.div(namedArgumentProvider: Pair<String, () -> Any?>): LogMessageBuilder {
        text(this)
        addNamedArgument(namedArgumentProvider.first, namedArgumentProvider.second)
        return this@LogMessageBuilderImpl
    }

    internal fun build() =
        if (messageSegments.isEmpty()) LogMessage.SimpleText("") else LogMessage.Structured(messageSegments)
}

internal fun buildLogMessage(logMessageProvider: LogMessageProvider) =
    try {
        val logMessageBuilder = LogMessageBuilderImpl()
        when (val logMessage = logMessageBuilder.logMessageProvider()) {
            is LogMessageBuilderImpl -> logMessage.build()
            else -> LogMessage.SimpleText(safeToString(logMessage))
        }
    } catch (e: Exception) {
        LogMessage.SimpleText(FailedEvaluationPlaceholder(e).toString())
    }
