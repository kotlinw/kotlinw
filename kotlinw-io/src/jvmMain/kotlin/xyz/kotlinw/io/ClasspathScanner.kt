package xyz.kotlinw.io

import io.github.classgraph.ClassGraph
import kotlinx.io.RawSource
import kotlinx.io.asSource

sealed interface ClasspathScanningContext

private object ClasspathScanningContextMarker : ClasspathScanningContext

@JvmInline
value class ScannedClasspathResource(
    private val nativeResource: io.github.classgraph.Resource
) {

    val path get() = AbsolutePath(nativeResource.path)

    val name get() = AbsolutePath(path.lastSegment)

    fun length() =
        nativeResource.open().use { nativeResource.length }
            .let {
                if (it == -1L) {
                    nativeResource.readCloseable().use { nativeResource.length }
                } else {
                    it
                }
            }

    fun loadUtf8String(): String = nativeResource.contentAsString

    fun readByteArray(): ByteArray = nativeResource.load()

    fun openAsSource(): RawSource = nativeResource.open().asSource()
}

interface ClasspathScanner {

    suspend fun <T> scanDirectory(
        directoryPath: ClasspathLocation,
        classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
        resourceProcessor: suspend ClasspathScanningContext.(ScannedClasspathResource) -> T
    ): Map<ClasspathLocation, T>

    suspend fun <T> scanResource(
        resourcePath: ClasspathLocation,
        classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
        resourceProcessor: suspend ClasspathScanningContext.(ScannedClasspathResource) -> T
    ): List<T>
}

class ClasspathScannerImpl : ClasspathScanner {

    override suspend fun <T> scanDirectory(
        directoryPath: ClasspathLocation,
        classLoader: ClassLoader,
        resourceProcessor: suspend ClasspathScanningContext.(ScannedClasspathResource) -> T
    ) =
        buildMap {
            ClassGraph().overrideClassLoaders(classLoader).acceptPaths(directoryPath.path.value).scan().use {
                it.allResources.nonClassFilesOnly().forEach {
                    put(
                        ClasspathLocation(AbsolutePath(it.path)),
                        ClasspathScanningContextMarker.resourceProcessor(ScannedClasspathResource(it!!))
                    )
                }
            }
        }

    override suspend fun <T> scanResource(
        resourcePath: ClasspathLocation,
        classLoader: ClassLoader,
        resourceProcessor: suspend ClasspathScanningContext.(ScannedClasspathResource) -> T
    ) =
        buildList {
            ClassGraph()
                .overrideClassLoaders(classLoader)
                .acceptPaths(resourcePath.path.parent?.value ?: NormalizedPath.ROOT_PATH_STRING)
                .scan().use {
                    it.allResources.nonClassFilesOnly().forEach {
                        if (it.path == resourcePath.path.value) {
                            add(ClasspathScanningContextMarker.resourceProcessor(ScannedClasspathResource(it!!)))
                        }
                    }
                }
        }
}
