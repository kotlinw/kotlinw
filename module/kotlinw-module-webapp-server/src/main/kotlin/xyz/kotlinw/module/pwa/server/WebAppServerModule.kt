package xyz.kotlinw.module.pwa.server

import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.ComponentScan
import xyz.kotlinw.di.api.Module
import xyz.kotlinw.pwa.core.WebManifestAttributeProvider
import xyz.kotlinw.pwa.core.WebManifestFactory
import xyz.kotlinw.pwa.core.WebManifestFactoryImpl
import xyz.kotlinw.pwa.core.WebResourceRegistrant
import xyz.kotlinw.pwa.core.WebResourceRegistryImpl

@Module
@ComponentScan
class WebAppServerModule {

    @Component
    fun webManifestFactory(webManifestAttributeProvider: WebManifestAttributeProvider): WebManifestFactory =
        WebManifestFactoryImpl(webManifestAttributeProvider)

    @Component // TODO bind only to interface types
    fun webResourceRegistry(webResourceRegistrants: List<WebResourceRegistrant>) =
        WebResourceRegistryImpl(webResourceRegistrants)

    @Component
    fun webAppMainHttpControllerChecker(webAppMainHttpController: WebAppMainHttpController) = Unit
}
