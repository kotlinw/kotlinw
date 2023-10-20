package xyz.kotlinw.module.pwa.server

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton
import xyz.kotlinw.pwa.core.WebResourceLookup
import xyz.kotlinw.pwa.core.WebResourceRegistrant
import xyz.kotlinw.pwa.core.WebResourceRegistry
import xyz.kotlinw.pwa.core.WebResourceRegistryImpl
import xyz.kotlinw.pwa.core.WebManifestAttributeProvider
import xyz.kotlinw.pwa.core.WebManifestFactory
import xyz.kotlinw.pwa.core.WebManifestFactoryImpl

@Module
@ComponentScan
class PwaServerModule {

    @Singleton
    fun webManifestFactory(webManifestAttributeProvider: WebManifestAttributeProvider): WebManifestFactory =
        WebManifestFactoryImpl(webManifestAttributeProvider)

    @Singleton(binds = [WebResourceRegistry::class, WebResourceLookup::class]) // TODO https://github.com/InsertKoinIO/koin/issues/1681
    fun webResourceRegistry(webResourceRegistrants: List<WebResourceRegistrant>): WebResourceRegistry =
        WebResourceRegistryImpl(webResourceRegistrants)
}
