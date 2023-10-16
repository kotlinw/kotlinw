package xyz.kotlinw.pwa.core

import xyz.kotlinw.io.RelativePath
import xyz.kotlinw.io.Resource

interface WebResourceLookup {

    fun getResourceWebPath(resource: Resource): RelativePath?
}
