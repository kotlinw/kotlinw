package kotlinw.module.serverbase

import io.ktor.server.application.Application

abstract class KtorServerApplicationConfigurer  {

    @PublishedApi
    internal fun setupModule(application: Application) = application.setup()

    // TODO replace with context(Application)
    abstract fun Application.setup()
}

fun KtorServerApplicationConfigurer(block: Application.() -> Unit) = object: KtorServerApplicationConfigurer() {

    override fun Application.setup() {
        block()
    }
}
