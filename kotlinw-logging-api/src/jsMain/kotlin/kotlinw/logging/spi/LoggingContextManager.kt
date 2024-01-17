package kotlinw.logging.spi

actual interface LoggingContextManager {

    @Deprecated(message = "Not supported on JS platform", level = DeprecationLevel.ERROR)
    actual fun <T> withNonSuspendableLoggingContext(contextChangeMap: Map<String, String?>, block: () -> T): T

    actual suspend fun <T> withLoggingContext(
        contextChangeMap: Map<String, String?>,
        block: suspend () -> T
    ): T
}
