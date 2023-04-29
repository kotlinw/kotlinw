package kotlinw.module.core.impl

import kotlinw.module.core.api.ApplicationCoroutineService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

@OptIn(ExperimentalStdlibApi::class)
class ApplicationCoroutineServiceImpl : ApplicationCoroutineService, AutoCloseable {

    override val coroutineScope = CoroutineScope(SupervisorJob())

    override fun close() {
        coroutineScope.cancel("Application shutdown is in progress.")
    }
}
