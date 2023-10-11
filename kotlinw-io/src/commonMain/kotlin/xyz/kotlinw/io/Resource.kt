package xyz.kotlinw.io

import kotlinx.io.RawSource

interface Resource {

    val name: String

    fun getContents(): RawSource

    fun exists(): Boolean

    override fun toString(): String
}

class ResourceNotFoundException(val resource: Resource): RuntimeException("Resource not found: $resource")
