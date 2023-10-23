package xyz.kotlinw.io

import io.github.classgraph.ClassGraph
import io.github.classgraph.ResourceList
import io.github.classgraph.ScanResult
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.asSource
import io.github.classgraph.Resource as ClassGraphResource

class ClasspathScanResult(
    private val path: AbsolutePath,
    private val classLoader: ClassLoader,
    private val nativeScanResult: ScanResult,
    private val nativeResources: ResourceList
) : AutoCloseable {

    val resources = nativeResources.map { ResolvedClasspathResource(path, it) }

    override fun close() {
        try {
            nativeScanResult.close()
        } catch (e: Exception) {
            // TODO log
        }
    }
}

data class ResolvedClasspathResource(val path: AbsolutePath, private val classGraphResource: ClassGraphResource) : Resource {

    companion object {

        fun findClasspathResource(
            path: AbsolutePath,
            classLoader: ClassLoader = Thread.currentThread().contextClassLoader ?: ClassLoader.getSystemClassLoader()
        ): ClasspathScanResult {
            val nativeScanResult =
                ClassGraph().overrideClassLoaders(classLoader).acceptPaths(path.parent!!.value).scan()
            val nativeResources = nativeScanResult.allResources.filter { it.path == path.value }
            return ClasspathScanResult(path, classLoader, nativeScanResult, nativeResources)
        }

        fun scanClasspathResources(
            path: AbsolutePath,
            classLoader: ClassLoader = Thread.currentThread().contextClassLoader ?: ClassLoader.getSystemClassLoader()
        ): ClasspathScanResult {
            val nativeScanResult =
                ClassGraph().overrideClassLoaders(classLoader).acceptPaths(path.value).scan()
            val nativeResources =
                nativeScanResult.allResources.filter { !it.path.endsWith(".class") }
            return ClasspathScanResult(path, classLoader, nativeScanResult, nativeResources)
        }
    }

    private class ClasspathScanResultBasedRawSource(
        private val classGraphScanResult: ClassGraphScanResult
    ) : RawSource {

        private val resourceRawSource = classGraphScanResult.classGraphResource.open().asSource()

        override fun readAtMostTo(sink: Buffer, byteCount: Long): Long = resourceRawSource.readAtMostTo(sink, byteCount)

        override fun close() {
            try {
                resourceRawSource.close()
            } finally {
                classGraphScanResult.close()
            }
        }
    }

    private class ClassGraphScanResult(val scanResult: ScanResult, val classGraphResource: ClassGraphResource) :
        AutoCloseable {

        override fun close() {
            try {
                classGraphResource.close()
            } catch (e: Exception) {
                // TODO log
            }

            try {
                scanResult.close()
            } catch (e: Exception) {
                // TODO log
            }
        }
    }

    override val name: String get() = path.lastSegment

    override fun open(): RawSource =
        try {
            getClassGraphResource()?.let { ClasspathScanResultBasedRawSource(it) }
                ?: throw ResourceResolutionException(this, "Resource not found or not accessible.")
        } catch (e: Exception) {
            throw ResourceResolutionException(this, "Failed to read resource contents.", e)
        }

    override fun exists(): Boolean =
        try {
            getClassGraphResource()?.use { true } ?: false
        } catch (e: Exception) {
            // TODO log trace
            false
        }

    override fun length(): Long? = getClassGraphResource()?.use { it.classGraphResource.length }

    override fun toString(): String = "ClassPathResource(path='$path', classLoader=$classLoader)"
}
