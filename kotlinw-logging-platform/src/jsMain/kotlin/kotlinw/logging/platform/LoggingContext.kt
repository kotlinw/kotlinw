package kotlinw.logging.platform

@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated(message = "Not supported on JS platform", level = DeprecationLevel.ERROR)
actual fun <T> withLoggingContext(
    contextChangeMap: Map<String, String?>,
    block: () -> T
): T =
    throw UnsupportedOperationException()
