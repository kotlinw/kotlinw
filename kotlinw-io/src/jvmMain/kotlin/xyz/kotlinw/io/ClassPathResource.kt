package xyz.kotlinw.io

import kotlinx.io.RawSource
import kotlinx.io.asSource
import java.io.InputStream

data class ClassPathResource(
    val path: AbsolutePath,
    val classLoader: ClassLoader = Thread.currentThread().contextClassLoader
) : Resource {

    override val name: String get() = path.lastSegment

    override fun getContents(): RawSource =
        classLoader.getResourceAsStream(path.value)?.asSource()
            ?: throw ResourceNotFoundException(this)

    override fun exists(): Boolean = classLoader.getResource(path.value) != null

    override fun toString(): String = "ClassPathResource(path='$path', classLoader=$classLoader)"
}
