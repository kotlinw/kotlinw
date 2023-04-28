package kotlinw.logging.api

import kotlin.jvm.JvmInline

@JvmInline
value class SimpleMarker(val name: String) : LogEntryAttribute
