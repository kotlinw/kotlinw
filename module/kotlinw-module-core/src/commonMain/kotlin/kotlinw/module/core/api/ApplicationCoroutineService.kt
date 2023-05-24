package kotlinw.module.core.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

interface ApplicationCoroutineService {

    val coroutineScope: CoroutineScope
}
