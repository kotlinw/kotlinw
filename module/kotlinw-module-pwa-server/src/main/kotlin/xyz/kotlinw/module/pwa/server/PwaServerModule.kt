package xyz.kotlinw.module.pwa.server

import io.ktor.http.ContentType
import io.ktor.server.application.call
import io.ktor.server.request.acceptLanguageItems
import io.ktor.server.response.respondText
import io.ktor.server.routing.accept
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinw.configuration.core.DeploymentMode
import kotlinw.configuration.core.DeploymentMode.Development
import kotlinw.i18n.ApplicationLocaleService
import kotlinw.i18n.LocaleId
import kotlinw.i18n.LocaleIds
import kotlinw.i18n.findBestSupportedLocale
import kotlinw.module.serverbase.KtorServerApplicationConfigurer
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton
import xyz.kotlinw.pwa.WebResourceRegistry
import xyz.kotlinw.pwa.WebResourceRegistryImpl
import xyz.kotlinw.pwa.core.WebManifestAttributeProvider
import xyz.kotlinw.pwa.core.WebManifestFactory
import xyz.kotlinw.pwa.core.WebManifestFactoryImpl

@Module
@ComponentScan
class PwaServerModule {

    @Singleton
    fun webManifestFactory(webManifestAttributeProvider: WebManifestAttributeProvider): WebManifestFactory =
        WebManifestFactoryImpl(webManifestAttributeProvider)

    @Singleton
    fun webResourceRegistry(): WebResourceRegistry = WebResourceRegistryImpl()
}

@Singleton
class WebManifestHttpController(
    private val webManifestFactory: WebManifestFactory,
    private val applicationLocaleService: ApplicationLocaleService,
    private val deploymentMode: DeploymentMode
) : KtorServerApplicationConfigurer() {

    override fun Context.setup() {
        application.routing {
            get("/manifest.webmanifest") {
                val localeId =
                    applicationLocaleService.findBestSupportedLocale(
                        call.request.acceptLanguageItems()
                            .filter { it.value != "*" }
                            .map { LocaleIds.of(it.value, applicationLocaleService.fallbackLocaleId) }
                    )
                call.respondText(
                    webManifestFactory.createWebManifest(localeId).serializeToString(),
                    ContentType.parse("application/manifest+json")
                )
            }
            route("/") {
                // TODO további oldalakat
                // TODO átnézni: https://gist.github.com/hal0gen/5852bd9db240c477f20c
                val title = "PWA" // TODO paraméterként + i18n
                val applicationFilePath = if (deploymentMode == Development)
                    "/appman-hub-webapp.js" // TODO általánosabb, pl. app.js
                else
                    TODO()
                get {
                    // TODO html/lang
                    // TODO theme-color
                    // TODO további meta tag-ek
                    // TODO service worker registration in 'load' event: https://web.dev/articles/service-workers-registration
                    // TODO service worker
//                    <script>
//                        if('serviceWorker' in navigator) {
//                            navigator.serviceWorker.register('/js/sw.js');
//                        };
//                    </script>
// TODO splash
                    call.respondText(
                        """
                            <!doctype html>
                            <html lang="hu">
                                <head>
                                    <meta charset="UTF-8">
                                    <meta name="viewport" content="width=device-width; initial-scale=1.0">
                                    <meta name="theme-color" content="#000000" />
                                    <meta http-equiv="X-UA-Compatible" content="ie=edge">
                                    <link rel="manifest" href="/manifest.webmanifest">
                                    <title>$title</title>
                                </head>
                                <body>
                                    <div id="root"></div>
                                    <script src="$applicationFilePath"></script>
                                </body>
                            </html>
                        """.trimIndent(),
                        ContentType.Text.Html
                    )
                }
            }
        }
    }
}
