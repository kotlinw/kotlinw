package xyz.kotlinw.pwa.core

import xyz.kotlinw.io.ClasspathLocation
import xyz.kotlinw.io.ClasspathResource
import xyz.kotlinw.io.RelativePath
import xyz.kotlinw.io.Resource
import xyz.kotlinw.io.append
import xyz.kotlinw.io.asRelativeTo
import xyz.kotlinw.io.isDescendantOf

class ClasspathFolderWebResourceMapping(
    val folderWebBasePath: RelativePath,
    val classpathFolderPath: ClasspathLocation
) : BuiltInWebResourceMapping {

    override fun getResourceWebPath(resource: Resource): RelativePath? =
        if (resource is ClasspathResource && resource.classpathLocation.path.isDescendantOf(classpathFolderPath.path))
            folderWebBasePath.append(resource.classpathLocation.path.asRelativeTo(classpathFolderPath.path))
        else
            null
}
