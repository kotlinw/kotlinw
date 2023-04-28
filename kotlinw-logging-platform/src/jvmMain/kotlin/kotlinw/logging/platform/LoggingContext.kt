package kotlinw.logging.platform

actual fun <T> withLoggingContext(
    contextChangeMap: Map<String, String?>,
    block: () -> T
): T =
    PlatformLogging.withLoggingContext(contextChangeMap, block)
