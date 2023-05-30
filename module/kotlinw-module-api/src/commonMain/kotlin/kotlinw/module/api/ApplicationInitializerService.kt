package kotlinw.module.api

import kotlinw.util.stdlib.HasPriority
import kotlinw.util.stdlib.Priority

abstract class ApplicationInitializerService(override val priority: Priority) : HasPriority {

    abstract fun performInitialization()
}

fun ApplicationInitializerService(priority: Priority, performInitialization: () -> Unit) =
    object : ApplicationInitializerService(priority) {

        override fun performInitialization() {
            performInitialization()
        }
    }
