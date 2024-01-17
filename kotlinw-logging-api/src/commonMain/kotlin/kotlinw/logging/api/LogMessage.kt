package kotlinw.logging.api

import kotlin.DeprecationLevel.ERROR
import kotlin.jvm.JvmInline

sealed interface LogMessage {

    data class FailedEvaluationPlaceholder(val exception: Throwable) {

        override fun toString(): String = "<failed: ${exception::class}: ${exception.message}>"
    }

    data object Empty: LogMessage

    @JvmInline
    value class SimpleText(val messageText: String) : LogMessage

    @JvmInline
    value class SimpleValue(val value: Any?) : LogMessage

    @JvmInline
    value class Structured(val segments: List<Segment>) : LogMessage {

        internal constructor(vararg segments: Segment) : this(segments.toList())

        sealed interface Segment {

            @JvmInline
            value class Text(val text: String) : Segment

            @JvmInline
            value class Value(val value: Any?) : Segment

            data class NamedValue(val name: String, val value: Any?) : Segment
        }
    }
}
