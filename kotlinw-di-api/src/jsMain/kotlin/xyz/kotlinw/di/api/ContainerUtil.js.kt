package xyz.kotlinw.di.api

import kotlinw.util.stdlib.DelicateKotlinwApi
import kotlinx.coroutines.delay

@OptIn(DelicateKotlinwApi::class)
suspend fun <T : ContainerScope> runJsApplication(
    rootScopeFactory: () -> T,
    block: suspend T.() -> Unit = { delay(Long.MAX_VALUE) }
) =
    runApplication(
        rootScopeFactory = rootScopeFactory,
        block = {
            block()
            delay(Long.MAX_VALUE)
        }
    )
