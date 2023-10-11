package xyz.kotlinw.pwa

import xyz.kotlinw.io.AbsolutePath
import xyz.kotlinw.io.ClassPathResource
import xyz.kotlinw.io.RelativePath
import xyz.kotlinw.io.Resource
import xyz.kotlinw.io.append
import xyz.kotlinw.io.asRelativeTo
import xyz.kotlinw.io.isDescendantOf

class ClasspathResourceWebPathLookup(
    private val folderWebBasePath: RelativePath,
    private val classpathFolderPath: AbsolutePath
) :
    ResourceWebPathLookup {

    override fun getResourceWebPath(resource: Resource): RelativePath? =
        if (resource is ClassPathResource && resource.path.isDescendantOf(classpathFolderPath) && resource.exists())
            folderWebBasePath.append(resource.path.asRelativeTo(classpathFolderPath))
        else
            null
}
