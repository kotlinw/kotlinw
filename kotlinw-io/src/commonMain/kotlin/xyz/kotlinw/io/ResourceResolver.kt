package xyz.kotlinw.io

import kotlinx.io.RawSource

interface ResourceResolver<in T: Resource> {

    fun open(resource: T): RawSource

    fun exists(resource: T): Boolean

    fun length(resource: T): Long?
}
