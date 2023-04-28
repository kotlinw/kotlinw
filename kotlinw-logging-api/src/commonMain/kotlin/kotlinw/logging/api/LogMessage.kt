package kotlinw.logging.api

import kotlin.jvm.JvmInline

sealed interface LogMessage {

    @JvmInline
    value class SimpleText(val messageText: String) : LogMessage

    data class Structured(val segments: List<Segment>) : LogMessage {

        internal constructor(vararg segments: Segment): this(segments.toList())

        sealed interface Segment {

            @JvmInline
            value class Text(val text: String) : Segment

            @JvmInline
            value class Value(val value: Any?) : Segment

            data class NamedValue(val name: String, val value: Any?) : Segment
        }
    }
}
