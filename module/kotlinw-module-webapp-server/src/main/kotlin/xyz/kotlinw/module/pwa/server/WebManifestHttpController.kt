package xyz.kotlinw.module.pwa.server

import io.ktor.http.ContentType
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.module.ktor.server.KtorServerApplicationConfigurer
import xyz.kotlinw.pwa.core.WebManifestFactory

@Component
class WebManifestHttpController(
    private val webManifestFactory: WebManifestFactory,
    private val webAppServerEnvironmentProvider: WebAppServerEnvironmentProvider
) : KtorServerApplicationConfigurer() {

    companion object {

        val webManifestcontentType = ContentType.parse("application/manifest+json")
    }

    override fun Context.setup() {
        ktorApplication.routing {
            get("/manifest.webmanifest") {
                call.respondText(
                    webManifestFactory.createWebManifest(
                        with(call) { webAppServerEnvironmentProvider.localeId }
                    ).serializeToString(),
                    webManifestcontentType
                )
            }
        }
    }
}
