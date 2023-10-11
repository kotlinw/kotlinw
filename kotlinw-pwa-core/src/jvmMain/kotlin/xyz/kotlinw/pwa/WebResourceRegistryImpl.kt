package xyz.kotlinw.pwa

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.io.files.FileSystem
import xyz.kotlinw.io.AbsolutePath
import xyz.kotlinw.io.RelativePath
import xyz.kotlinw.io.Resource

class WebResourceRegistryImpl : WebResourceRegistry {

    private val resourceWebPathLookups = atomic(persistentListOf<ResourceWebPathLookup>())

    override fun resolveWebUrl(resource: Resource): RelativePath? {
        resourceWebPathLookups.value.forEach {
            val webPath = it.getResourceWebPath(resource)
            if (webPath != null) {
                return webPath
            }
        }

        return null
    }

    override fun registerClasspathFolder(
        folderWebBasePath: RelativePath,
        classLoader: ClassLoader,
        classpathFolderPath: AbsolutePath
    ) {
        resourceWebPathLookups.update {
            it.mutate {
                it.add(ClasspathResourceWebPathLookup(folderWebBasePath, classpathFolderPath))
            }
        }
    }
}
