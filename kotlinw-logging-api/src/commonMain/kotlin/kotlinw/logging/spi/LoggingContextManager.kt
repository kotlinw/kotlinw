package kotlinw.logging.spi

expect interface LoggingContextManager {

    fun <T> withLoggingContext(contextChangeMap: Map<String, String?>, block: () -> T): T

    suspend fun <T> withCoroutineLoggingContext(contextChangeMap: Map<String, String?>, block: suspend () -> T): T
}
