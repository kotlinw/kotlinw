package xyz.kotlinw.module.serverbase.api

import xyz.kotlinw.di.api.Module
import xyz.kotlinw.module.appbase.api.AppbaseModule
import xyz.kotlinw.module.ktor.server.KtorServerModule
import xyz.kotlinw.module.remoting.server.ServerRemotingModule

@Module(includeModules = [KtorServerModule::class, ServerRemotingModule::class, AppbaseModule::class])
class ServerBaseModule
