package xyz.kotlinw.io

import kotlinx.io.Source
import kotlinx.io.buffered
import java.util.concurrent.atomic.AtomicBoolean

class ClasspathResource(
    private val classpathScanner: ClasspathScanner,
    val classpathLocation: ClasspathLocation,
    val classLoader: ClassLoader = Thread.currentThread().contextClassLoader
) : Resource {

    override val name: String get() = classpathLocation.name

    override fun toString(): String = classpathLocation.toString()

    private suspend fun <T> performOperation(resourceProcessor: suspend ClasspathScanningContext.(ScannedClasspathResource) -> T) =
        classpathScanner.scanResource(classpathLocation, classLoader, resourceProcessor)

    override suspend fun <T> useAsSource(block: suspend (Source) -> T): T =
        performOperation {
            it.openAsSource().buffered().use {
                block(it)
            }
        }.let {
            if (it.size == 1) {
                it.first()
            } else {
                throw IllegalStateException() // TODO
            }
        }

    override suspend fun exists(): Boolean {
        val exists = AtomicBoolean(false)
        performOperation {
            exists.compareAndSet(false, true)
        }
        return exists.get()
    }

    override suspend fun length(): Long? =
        performOperation {
            it.length()
        }.let {
            if (it.size == 1) {
                it.first()
            } else {
                null
            }
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ClasspathResource) return false

        if (classpathLocation != other.classpathLocation) return false
        if (classLoader != other.classLoader) return false

        return true
    }

    override fun hashCode(): Int {
        var result = classpathLocation.hashCode()
        result = 31 * result + classLoader.hashCode()
        return result
    }
}
