package kotlinw.logging.spi

// Source: https://github.com/oshai/kotlin-logging/blob/master/src/javaMain/kotlin/io/github/oshai/internal/KLoggerNameResolver.kt
@Suppress("NOTHING_TO_INLINE")
@PublishedApi
internal actual inline fun resolveLoggerName(noinline function: () -> Unit): String? {
    val name = function.javaClass.name
    return when {
        name.contains("Kt$") -> name.substringBefore("Kt$")
        name.contains("$") -> name.substringBefore("$")
        else -> name
    }
}
