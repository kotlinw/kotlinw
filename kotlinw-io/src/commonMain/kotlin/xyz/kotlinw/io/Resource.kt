package xyz.kotlinw.io

import kotlinx.io.Source
import kotlinx.io.readByteArray

interface Resource {

    val name: String

    override fun toString(): String

    suspend fun <T> useAsSource(block: suspend (Source) -> T): T

    suspend fun exists(): Boolean

    suspend fun length(): Long?
}

suspend fun Resource.readByteArray(): ByteArray = useAsSource { it.readByteArray() }

suspend fun Resource.readUtf8String(): String = readByteArray().decodeToString()
