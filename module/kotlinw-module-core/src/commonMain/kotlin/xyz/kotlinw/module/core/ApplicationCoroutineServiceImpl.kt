package xyz.kotlinw.module.core

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

    override val applicationCoroutineScope =
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
        applicationCoroutineScope.cancel("Application shutdown is in progress.")
    }
}
