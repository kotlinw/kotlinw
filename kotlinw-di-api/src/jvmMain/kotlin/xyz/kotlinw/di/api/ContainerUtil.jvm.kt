package xyz.kotlinw.di.api

import kotlinw.util.stdlib.createPidFile
import kotlinw.util.stdlib.deletePidFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

// TODO ennek inkább az appbase modulban lenne a helye
suspend fun <T : ContainerScope> runJvmApplication(
    rootScopeFactory: () -> T,
    args: Array<out String> = emptyArray(), // TODO ezt passzoljuk tovább
    block: suspend T.() -> Unit = { delay(Long.MAX_VALUE) }
) =
    runApplication(
        rootScopeFactory,
        ::createPidFile,
        {
            Runtime.getRuntime().addShutdownHook(
                Thread {
                    try {
                        runBlocking {
                            it.close()
                        }
                    } finally {
                        deletePidFile()
                    }
                }
            )
        },
        ::deletePidFile,
        block
    )
