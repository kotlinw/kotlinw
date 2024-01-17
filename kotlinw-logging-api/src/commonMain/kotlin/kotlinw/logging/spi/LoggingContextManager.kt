package kotlinw.logging.spi

expect interface LoggingContextManager {

    fun <T> withNonSuspendableLoggingContext(contextChangeMap: Map<String, String?>, block: () -> T): T

    suspend fun <T> withLoggingContext(contextChangeMap: Map<String, String?>, block: suspend () -> T): T
}
