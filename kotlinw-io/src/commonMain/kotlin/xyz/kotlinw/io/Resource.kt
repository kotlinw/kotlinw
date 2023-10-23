package xyz.kotlinw.io

import kotlinx.io.RawSource

interface Resource {

    val name: String

    fun open(): RawSource

    fun exists(): Boolean

    fun length(): Long?

    override fun toString(): String
}
