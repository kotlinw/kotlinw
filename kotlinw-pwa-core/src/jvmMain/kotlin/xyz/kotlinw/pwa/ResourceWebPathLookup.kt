package xyz.kotlinw.pwa

import xyz.kotlinw.io.AbsolutePath
import xyz.kotlinw.io.ClassPathResource
import xyz.kotlinw.io.RelativePath
import xyz.kotlinw.io.Resource
import xyz.kotlinw.io.append
import xyz.kotlinw.io.asRelativeTo
import xyz.kotlinw.io.isDescendantOf

interface ResourceWebPathLookup {

    fun getResourceWebPath(resource: Resource): RelativePath?
}
