package kotlinw.logging.spi

// Source: https://github.com/oshai/kotlin-logging/blob/master/src/nativeMain/kotlin/io/github/oshai/internal/KLoggerNameResolver.kt
@PublishedApi
internal actual inline fun resolveLoggerName(noinline function: () -> Unit): String? =
    function::class.qualifiedName ?: ""
