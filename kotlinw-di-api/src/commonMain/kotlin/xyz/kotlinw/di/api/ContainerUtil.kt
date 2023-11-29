package xyz.kotlinw.di.api

import kotlinx.coroutines.delay

// TODO non-public API
// TODO hibakezelés
suspend fun <T : ContainerScope> runApplication(
    rootScopeFactory: () -> T,
    beforeScopeCreated: () -> Unit = {},
    afterUninitializedScopeCreated: (T) -> Unit = {},
    shutdown: () -> Unit = {},
    block: suspend T.() -> Unit = { delay(Long.MAX_VALUE) }
) {
    beforeScopeCreated()
    val rootScope = rootScopeFactory()
    afterUninitializedScopeCreated(rootScope)

    try {
        with(rootScope) {
            try {
                start()
                block()
            } finally {
                close()
            }
        }
    } finally {
        shutdown()
    }
}
