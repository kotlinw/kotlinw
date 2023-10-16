package xyz.kotlinw.module.pwa.server

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion
import io.ktor.http.content.OutgoingContent
import io.ktor.http.defaultForFilePath
import io.ktor.server.application.call
import io.ktor.server.http.content.staticResources
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinw.module.serverbase.KtorServerApplicationConfigurer
import kotlinx.io.asSink
import kotlinx.io.buffered
import org.koin.core.annotation.Singleton
import xyz.kotlinw.pwa.core.BuiltInWebResourceMapping
import xyz.kotlinw.pwa.core.ClasspathFolderWebResourceMapping
import xyz.kotlinw.pwa.core.ResourceWebResourceMapping
import xyz.kotlinw.pwa.core.WebResourceRegistry

@Singleton
class BuiltInWebResourceSupportController(private val webResourceRegistry: WebResourceRegistry) :
    KtorServerApplicationConfigurer() {

    override fun Context.setup() {
        application.routing {
            webResourceRegistry.webResourceMappings.filterIsInstance<BuiltInWebResourceMapping>().forEach {
                when (it) {
                    is ClasspathFolderWebResourceMapping -> {
                        // TODO http caching
                        staticResources(it.folderWebBasePath.value, it.classpathFolderPath.value, null)
                    }

                    is ResourceWebResourceMapping -> {
                        // TODO http caching

                        val resource = it.resource
                        if (resource.exists()) {
                            val length = resource.length()
                            get(it.fileWebPath.value) {
                                call.respond(
                                    OutputStreamContentWithLength(
                                        ContentType.defaultForFilePath(resource.name),
                                        length
                                    ) {
                                        resource.getContents().use {
                                            asSink().buffered().apply {
                                                transferFrom(it)
                                                flush()
                                            }
                                        }
                                    }
                                )
                            }
                        } else {
                            get(it.fileWebPath.value) {
                                // TODO log
                                call.respond(Companion.NotFound)
                            }
                        }
                    }
                }
            }
        }
    }
}
