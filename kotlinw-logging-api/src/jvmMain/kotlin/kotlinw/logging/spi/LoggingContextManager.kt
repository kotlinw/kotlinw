package kotlinw.logging.spi

actual interface LoggingContextManager {

    actual fun <T> withLoggingContext(
        contextChangeMap: Map<String, String?>,
        block: () -> T
    ): T

    actual suspend fun <T> withCoroutineLoggingContext(
        contextChangeMap: Map<String, String?>,
        block: suspend () -> T
    ): T
}
