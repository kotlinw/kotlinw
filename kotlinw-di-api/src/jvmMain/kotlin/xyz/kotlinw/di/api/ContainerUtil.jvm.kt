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
) {
    println(System.getProperty("java.vendor")) // TODO remove
    println(System.getProperty("java.version")) // TODO remove

    runApplication(
        rootScopeFactory = rootScopeFactory,
        beforeScopeCreated = ::createPidFile,
        afterUninitializedScopeCreated = {
            Runtime.getRuntime().addShutdownHook(
                Thread {
                    // TODO log
                    try {
                        runBlocking {
                            try {
                                shutdownApplication(it, it.containerLifecycleCoordinator)
                            } catch (e: Throwable) {
                                e.printStackTrace()
                            }
                        }
                    } finally {
                        deletePidFile()
                    }
                }
            )
        },
        shutdown = ::deletePidFile,
        block = block
    )
}
