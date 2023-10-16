package xyz.kotlinw.pwa.core

import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.logging.platform.PlatformLogging
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentCollection
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import xyz.kotlinw.io.AbsolutePath
import xyz.kotlinw.io.RelativePath
import xyz.kotlinw.io.Resource
import xyz.kotlinw.pwa.core.WebResourceRegistrant.Context

class WebResourceRegistryImpl(
    webResourceRegistrants: List<WebResourceRegistrant>
) :
    WebResourceRegistry, WebResourceLookup {

    private val logger = PlatformLogging.getLogger()

    private val webResourceMappingsHolder = atomic(persistentListOf<WebResourceMapping>())

    override val webResourceMappings: ImmutableList<WebResourceMapping> get() = webResourceMappingsHolder.value

    init {
        webResourceRegistrants.forEach {
            try {
                it.registerWebResources(WebResourceRegistrantContext())
            } catch (e: Exception) {
                throw RuntimeException("${WebResourceRegistrant::class.simpleName} failed", e)
            }
        }
    }

    private inner class WebResourceRegistrantContext : Context {

        override fun registerClasspathFolder(
            folderWebBasePath: RelativePath,
            classpathFolderPath: AbsolutePath,
            classLoader: ClassLoader
        ) {
            logger.debug {
                "Registering classpath folder as web resource: " / mapOf(
                    "folderWebBasePath" to folderWebBasePath,
                    "classpathFolderPath" to classpathFolderPath
                )
            }
            webResourceMappingsHolder.update {
                it.add(ClasspathFolderWebResourceMapping(folderWebBasePath, classpathFolderPath))
            }
        }

        override fun registerResource(fileWebPath: RelativePath, resource: Resource) {
            logger.debug {
                "Registering resource as web resource: " / mapOf(
                    "fileWebPath" to fileWebPath,
                    "resource" to resource
                )
            }

// TODO dev mode
//            if (!resource.exists()) {
//                logger.warning {
//                    "Resource does not exist, it may result in HTTP 404 error: " / listOf(
//                        fileWebPath,
//                        resource
//                    )
//                }
//            }

            webResourceMappingsHolder.update {
                it.add(ResourceWebResourceMapping(fileWebPath, resource))
            }
        }
    }

    override fun getResourceWebPath(resource: Resource): RelativePath? {
        webResourceMappingsHolder.value.forEach {
            val webPath = it.getResourceWebPath(resource)
            if (webPath != null) {
                return webPath
            }
        }

        return null
    }
}
