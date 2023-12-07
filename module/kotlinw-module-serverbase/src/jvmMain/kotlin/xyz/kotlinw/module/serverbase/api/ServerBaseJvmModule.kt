package xyz.kotlinw.module.serverbase.api

import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngineFactory
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.Module
import xyz.kotlinw.module.appbase.api.AppbaseJvmModule
import xyz.kotlinw.module.ktor.server.KtorServerModule

@Module(includeModules = [AppbaseJvmModule::class, KtorServerModule::class, ServerBaseModule::class])
class ServerBaseJvmModule {

    @Component
    fun applicationEngineFactory(): ApplicationEngineFactory<*, *> = CIO
}
