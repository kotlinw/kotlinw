package xyz.kotlinw.pwa.core

import xyz.kotlinw.io.AbsolutePath
import xyz.kotlinw.io.ClasspathResource
import xyz.kotlinw.io.RelativePath
import xyz.kotlinw.io.Resource
import xyz.kotlinw.io.append
import xyz.kotlinw.io.asRelativeTo
import xyz.kotlinw.io.isDescendantOf

class ClasspathFolderWebResourceMapping(
    val folderWebBasePath: RelativePath,
    val classpathFolderPath: AbsolutePath
) : BuiltInWebResourceMapping {

    override fun getResourceWebPath(resource: Resource): RelativePath? =
        if (resource is ClasspathResource && resource.path.isDescendantOf(classpathFolderPath) && resource.exists())
            folderWebBasePath.append(resource.path.asRelativeTo(classpathFolderPath))
        else
            null
}
