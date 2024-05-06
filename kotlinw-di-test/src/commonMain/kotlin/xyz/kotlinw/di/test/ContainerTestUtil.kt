package xyz.kotlinw.di.test

import kotlinw.util.stdlib.DelicateKotlinwApi
import xyz.kotlinw.di.api.ContainerScope
import xyz.kotlinw.di.api.runApplication

@OptIn(DelicateKotlinwApi::class)
suspend fun <S : ContainerScope> runContainerTest(
    rootScopeFactory: () -> S,
    block: suspend S.() -> Unit
) {
    runApplication(
        rootScopeFactory = rootScopeFactory
    ) {
        block()
    }
}
