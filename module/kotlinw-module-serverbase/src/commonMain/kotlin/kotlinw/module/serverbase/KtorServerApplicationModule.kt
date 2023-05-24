package kotlinw.module.serverbase

import io.ktor.server.application.Application

abstract class KtorServerApplicationModule  {

    @PublishedApi
    internal fun setupModule(application: Application) = application.setup()

    // TODO replace with context(Application)
    abstract fun Application.setup()
}

fun KtorServerApplicationModule(block: Application.() -> Unit) = object: KtorServerApplicationModule() {

    override fun Application.setup() {
        block()
    }
}
