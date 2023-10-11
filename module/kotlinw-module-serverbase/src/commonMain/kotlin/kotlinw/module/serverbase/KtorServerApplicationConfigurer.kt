package kotlinw.module.serverbase

import io.ktor.server.application.Application
import kotlinw.module.serverbase.KtorServerApplicationConfigurer.Context
import kotlinw.util.stdlib.HasPriority
import kotlinw.util.stdlib.Priority
import kotlinx.coroutines.CoroutineScope

abstract class KtorServerApplicationConfigurer(override val priority: Priority = Priority.Normal) : HasPriority {

    data class Context(
        val application: Application,
        val ktorServerCoroutineScope: CoroutineScope
    )

    @PublishedApi
    internal fun setupModule(context: Context) = context.setup()

    // TODO replace with context(Context)
    abstract fun Context.setup()
}

fun KtorServerApplicationConfigurer(priority: Priority = Priority.Normal, block: Context.() -> Unit) =
    object : KtorServerApplicationConfigurer(priority) {

        override fun Context.setup() {
            block()
        }
    }
