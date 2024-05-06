package xyz.kotlinw.di.test

import kotlinw.util.stdlib.DelicateKotlinwApi
import xyz.kotlinw.di.api.ContainerScope
import xyz.kotlinw.di.api.ContainerScopeInternal
import xyz.kotlinw.di.api.runApplication
import xyz.kotlinw.di.impl.ContainerLifecycleCoordinatorImpl

@OptIn(DelicateKotlinwApi::class)
private suspend fun <S : ContainerScope> runContainerTest(
    rootScopeFactory: () -> S,
    afterScopeStarted: (S) -> Unit,
    startupTaskFilter: (String) -> Boolean,
    block: suspend S.() -> Unit
) {
    runApplication(
        rootScopeFactory = rootScopeFactory,
        afterScopeStarted = afterScopeStarted,
        startupTaskFilter = startupTaskFilter
    ) {
        block()
    }
}

suspend fun <S : ContainerScope> runContainerTest(
    rootScopeFactory: () -> S,
    disabledStartupTasks: Set<String> = emptySet(),
    block: suspend S.() -> Unit
) {
    runContainerTest(
        rootScopeFactory = rootScopeFactory,
        afterScopeStarted = { validateStartupTaskIds(it, disabledStartupTasks) },
        startupTaskFilter = { it !in disabledStartupTasks },
        block = block
    )
}

private fun <S : ContainerScope> validateStartupTaskIds(scope: S, startupTasksIds: Set<String>) {
    val coordinator = (scope as ContainerScopeInternal).containerLifecycleCoordinator as? ContainerLifecycleCoordinatorImpl
    if (coordinator != null) {
        startupTasksIds.forEach {
            check(it in coordinator.lifecycleListeners.map { it.getLifecycleListenerId() }) { "Invalid disabled startup task: $it" }
        }
    } else {
        require(startupTasksIds.isEmpty())
    }
}

suspend fun <S : ContainerScope> runContainerTestWithWithoutStartupTasks(
    rootScopeFactory: () -> S,
    enabledStartupTasks: Set<String> = emptySet(),
    block: suspend S.() -> Unit
) {
    runContainerTest(
        rootScopeFactory = rootScopeFactory,
        afterScopeStarted = { validateStartupTaskIds(it, enabledStartupTasks) },
        startupTaskFilter = { it in enabledStartupTasks },
        block = block
    )
}
