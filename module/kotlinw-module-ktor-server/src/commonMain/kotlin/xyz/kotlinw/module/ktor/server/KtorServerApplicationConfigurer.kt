package xyz.kotlinw.module.ktor.server

import io.ktor.server.application.Application
import xyz.kotlinw.module.ktor.server.KtorServerApplicationConfigurer.Context
import kotlinw.util.stdlib.HasPriority
import kotlinw.util.stdlib.Priority
import kotlinx.coroutines.CoroutineScope

abstract class KtorServerApplicationConfigurer(override val priority: Priority = Priority.Normal) : HasPriority {

    data class Context(
        val ktorApplication: Application,
        val ktorServerCoroutineScope: CoroutineScope
    )

    open suspend fun initializeKtorServerApplicationConfigurer() {}

    internal fun setupKtorModule(context: Context) = context.setup()

    // TODO replace with context(Context)
    abstract fun Context.setup()
}

fun KtorServerApplicationConfigurer(priority: Priority = Priority.Normal, block: Context.() -> Unit) =
    object : KtorServerApplicationConfigurer(priority) {

        override fun Context.setup() {
            block()
        }
    }
