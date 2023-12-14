package xyz.kotlinw.module.webapp.client

import xyz.kotlinw.di.api.ComponentScan
import xyz.kotlinw.di.api.Module
import xyz.kotlinw.module.appbase.api.AppbaseJsModule

@Module(includeModules = [AppbaseJsModule::class])
@ComponentScan
class WebAppClientModule
