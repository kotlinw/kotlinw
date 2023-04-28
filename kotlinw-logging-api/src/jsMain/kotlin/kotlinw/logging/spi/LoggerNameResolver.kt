package kotlinw.logging.spi

// Source: https://github.com/oshai/kotlin-logging/blob/master/src/jsMain/kotlin/io/github/oshai/internal/KLoggerNameResolver.kt
@Suppress("NOTHING_TO_INLINE")
@PublishedApi
internal actual inline fun resolveLoggerName(noinline function: () -> Unit): String? {
    var found = false
    val exception = Exception()
    for (line in exception.stackTraceToString().split("\n")) {
        if (found) {
            return line.substringBefore(".kt").substringAfterLast(".").substringAfterLast("/")
        }
        if (line.contains("at KotlinLogging")) {
            found = true
        }
    }
    return ""
}
