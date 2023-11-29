package xyz.kotlinw.di.api

import kotlinx.coroutines.delay

suspend fun <T : ContainerScope> runJsApplication(
    rootScopeFactory: () -> T,
    block: suspend T.() -> Unit = { delay(Long.MAX_VALUE) }
) =
    runApplication(rootScopeFactory, block = block)
