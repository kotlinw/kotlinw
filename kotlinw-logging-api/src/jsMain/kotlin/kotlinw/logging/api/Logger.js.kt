package kotlinw.logging.api

@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated(message = "Not supported on JS platform", level = DeprecationLevel.ERROR)
actual fun <T> Logger.withNonSuspendableLoggingContext(
    contextChangeMap: Map<String, String?>,
    block: () -> T
): T =
    throw UnsupportedOperationException()
