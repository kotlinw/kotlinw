package xyz.kotlinw.module.serverbase.api

import xyz.kotlinw.di.api.Module
import xyz.kotlinw.module.ktor.server.KtorServerModule

@Module(includeModules = [KtorServerModule::class, ServerRemotingModule::class])
class ServerBaseModule
