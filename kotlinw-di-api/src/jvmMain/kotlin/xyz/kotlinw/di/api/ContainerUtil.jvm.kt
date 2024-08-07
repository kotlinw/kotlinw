package xyz.kotlinw.di.api

import kotlinw.util.stdlib.DelicateKotlinwApi
import kotlinw.util.stdlib.createPidFile
import kotlinw.util.stdlib.deletePidFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

@OptIn(DelicateKotlinwApi::class)
suspend fun <S : ContainerScope, T> runJvmApplication(
    rootScopeFactory: () -> S,
    args: Array<out String> = emptyArray(), // TODO ezt passzoljuk tovább
    block: suspend S.() -> T
) =
    runApplication(
        rootScopeFactory = rootScopeFactory,
        beforeScopeCreated = ::createPidFile,
        afterUninitializedScopeCreated = {
            Runtime.getRuntime().addShutdownHook(
                Thread {
                    // TODO log
                    try {
                        runBlocking {
                            shutdownApplication(it)
                        }
                    } finally {
                        deletePidFile()
                    }
                }
            )
        },
        onShutdown = ::deletePidFile,
        block = block
    )

suspend fun <S : ContainerScope> runBackgroundServiceJvmApplication(
    rootScopeFactory: () -> S,
    args: Array<out String> = emptyArray() // TODO ezt passzoljuk tovább
) =
    runJvmApplication(rootScopeFactory, args) {
        delay(Long.MAX_VALUE)
    }
