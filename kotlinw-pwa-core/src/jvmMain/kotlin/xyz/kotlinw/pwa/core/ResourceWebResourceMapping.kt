package xyz.kotlinw.pwa.core

import xyz.kotlinw.io.RelativePath
import xyz.kotlinw.io.Resource

class ResourceWebResourceMapping(
    val fileWebPath: RelativePath,
    val resource: Resource
) : BuiltInWebResourceMapping {

    override fun getResourceWebPath(resource: Resource): RelativePath? =
        if (resource == this.resource)
            fileWebPath
        else
            null
}
