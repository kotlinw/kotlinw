package xyz.kotlinw.module.pwa.server

import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.ComponentScan
import xyz.kotlinw.di.api.Module
import xyz.kotlinw.module.ktor.server.KtorServerApplicationConfigurer
import xyz.kotlinw.pwa.core.WebResourceRegistrant
import xyz.kotlinw.pwa.core.WebResourceRegistry
import xyz.kotlinw.pwa.core.WebResourceRegistryImpl
import xyz.kotlinw.pwa.core.WebManifestAttributeProvider
import xyz.kotlinw.pwa.core.WebManifestFactory
import xyz.kotlinw.pwa.core.WebManifestFactoryImpl

@Module
@ComponentScan
class PwaServerModule {

    @Component
    fun webManifestFactory(webManifestAttributeProvider: WebManifestAttributeProvider): WebManifestFactory =
        WebManifestFactoryImpl(webManifestAttributeProvider)

    @Component // TODO bind to only interface types
    fun webResourceRegistry(webResourceRegistrants: List<WebResourceRegistrant>) =
        WebResourceRegistryImpl(webResourceRegistrants)
}
