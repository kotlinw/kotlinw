package xyz.kotlinw.pwa

import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.logging.platform.PlatformLogging
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import xyz.kotlinw.io.AbsolutePath
import xyz.kotlinw.io.RelativePath
import xyz.kotlinw.io.Resource

class WebResourceRegistryImpl(webResourceRegistrants: List<WebResourceRegistrant>) :
    WebResourceRegistry, ResourceWebPathLookup {

    private val logger = PlatformLogging.getLogger()

    private val resourceWebPathLookups = atomic(persistentListOf<ResourceWebPathLookup>())

    init {
        webResourceRegistrants.forEach {
            try {
                it.registerWebResources(WebResourceRegistrantContext())
            } catch (e: Exception) {
                throw RuntimeException("WebResourceRegistrant failed", e)
            }
        }
    }

    private inner class WebResourceRegistrantContext : WebResourceRegistrant.Context {

        override fun registerClasspathFolder(
            folderWebBasePath: RelativePath,
            classpathFolderPath: AbsolutePath,
            classLoader: ClassLoader
        ) {
            logger.debug {
                "Registering web resources in classpath folder: " / mapOf(
                    "folderWebBasePath" to folderWebBasePath,
                    "classpathFolderPath" to classpathFolderPath
                )
            }
            resourceWebPathLookups.update {
                it.mutate {
                    it.add(ClasspathResourceWebPathLookup(folderWebBasePath, classpathFolderPath))
                }
            }
        }
    }

    override fun getResourceWebPath(resource: Resource): RelativePath? {
        resourceWebPathLookups.value.forEach {
            val webPath = it.getResourceWebPath(resource)
            if (webPath != null) {
                return webPath
            }
        }

        return null
    }
}
