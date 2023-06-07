package kotlinw.koin.core.internal

import kotlinw.koin.core.api.ApplicationCoroutineService
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.logging.platform.PlatformLogging
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalStdlibApi::class)
class ApplicationCoroutineServiceImpl(
    parentJob: Job? = null
) : ApplicationCoroutineService, AutoCloseable {

    private val logger = PlatformLogging.getLogger()

    override val coroutineScope =
        CoroutineScope(
            SupervisorJob(parentJob) +
                    CoroutineExceptionHandler { coroutineContext, throwable ->
                        handleUncaughtCoroutineException(coroutineContext, throwable)
                    }
        )

    private fun handleUncaughtCoroutineException(coroutineContext: CoroutineContext, throwable: Throwable) {
        logger.error(throwable) { "Uncaught coroutine exception. " / named("coroutineContext", coroutineContext) }
    }

    override fun close() {
        coroutineScope.cancel("Application shutdown is in progress.")
    }

    override fun <T> runBlocking(block: suspend CoroutineScope.() -> T): T = runBlockingImpl(block)
}

internal expect fun <T> runBlockingImpl(block: suspend CoroutineScope.() -> T): T
