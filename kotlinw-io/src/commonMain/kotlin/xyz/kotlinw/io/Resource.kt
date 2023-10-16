package xyz.kotlinw.io

import kotlinx.io.RawSource

// TODO külön ResourceFolder és Resource
interface Resource {

    val name: String

    fun getContents(): RawSource

    fun exists(): Boolean

    fun length(): Long?

    override fun toString(): String
}

class ResourceNotFoundException(val resource: Resource): RuntimeException("Resource not found: $resource")
