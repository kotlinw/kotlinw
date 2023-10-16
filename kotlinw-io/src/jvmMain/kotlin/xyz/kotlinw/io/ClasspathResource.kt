package xyz.kotlinw.io

import kotlinx.io.RawSource
import kotlinx.io.asSource
import kotlinx.io.buffered

data class ClasspathResource(
    val path: AbsolutePath,
    val classLoader: ClassLoader = Thread.currentThread().contextClassLoader ?: ClassLoader.getSystemClassLoader()
) : Resource {

    override val name: String get() = path.lastSegment

    override fun getContents(): RawSource =
        classLoader.getResourceAsStream(path.value)?.asSource()
            ?: throw ResourceNotFoundException(this)

    override fun exists(): Boolean = classLoader.getResource(path.value) != null

    override fun length(): Long {
        var size = 0L
        getContents().buffered().use {
            while (!it.exhausted()) {
                it.readByte()
                size++
            }
        }
        return size
    }

    override fun toString(): String = "ClassPathResource(path='$path', classLoader=$classLoader)"
}
