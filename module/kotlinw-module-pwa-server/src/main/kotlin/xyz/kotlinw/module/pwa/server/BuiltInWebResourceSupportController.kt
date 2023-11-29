package xyz.kotlinw.module.pwa.server

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.defaultForFilePath
import io.ktor.server.application.call
import io.ktor.server.http.content.staticResources
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.io.asSink
import kotlinx.io.buffered
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.module.ktor.server.KtorServerApplicationConfigurer
import xyz.kotlinw.pwa.core.BuiltInWebResourceMapping
import xyz.kotlinw.pwa.core.ClasspathFolderWebResourceMapping
import xyz.kotlinw.pwa.core.ResourceWebResourceMapping
import xyz.kotlinw.pwa.core.WebResourceRegistry

@Component
class BuiltInWebResourceSupportController(
    private val webResourceRegistry: WebResourceRegistry
) :
    KtorServerApplicationConfigurer() {

    override fun Context.setup() {
        application.routing {
            webResourceRegistry.webResourceMappings.filterIsInstance<BuiltInWebResourceMapping>()
                .forEach { webResourceMapping ->
                    when (webResourceMapping) {
                        is ClasspathFolderWebResourceMapping -> {
                            // TODO http caching
                            staticResources(
                                webResourceMapping.folderWebBasePath.value,
                                webResourceMapping.classpathFolderPath.path.value,
                                null
                            )
                        }

                        is ResourceWebResourceMapping -> {
                            // TODO http caching

                            get(webResourceMapping.fileWebPath.value) {
                                val resource = webResourceMapping.resource
                                if (resource.exists()) {
                                    val length = resource.length()
                                    try {
                                        resource.useAsSource { source ->
                                            call.respond(
                                                OutputStreamContentWithLength(
                                                    ContentType.defaultForFilePath(resource.name),
                                                    length
                                                ) {
                                                    asSink().buffered().apply {
                                                        transferFrom(source)
                                                        flush()
                                                    }
                                                }
                                            )
                                        }
                                    } catch (e: Exception) {
                                        // TODO log
                                        call.respond(HttpStatusCode.NotFound)
                                    }
                                } else {
                                    // TODO log
                                    call.respond(HttpStatusCode.NotFound)
                                }
                            }
                        }
                    }
                }
        }
    }
}
