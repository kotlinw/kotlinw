package kotlinw.module.api

import kotlinw.util.stdlib.HasPriority
import kotlinw.util.stdlib.Priority

abstract class ApplicationInitializerService(override val priority: Priority) : HasPriority {

    abstract suspend fun performInitialization()
}

fun ApplicationInitializerService(priority: Priority, performInitialization: suspend () -> Unit) =
    object : ApplicationInitializerService(priority) {

        override suspend fun performInitialization() {
            performInitialization()
        }
    }
