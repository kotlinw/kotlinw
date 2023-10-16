package xyz.kotlinw.pwa.core

import xyz.kotlinw.io.AbsolutePath
import xyz.kotlinw.io.RelativePath
import xyz.kotlinw.io.Resource

fun interface WebResourceRegistrant {

    interface Context {

        fun registerClasspathFolder(
            folderWebBasePath: RelativePath,
            classpathFolderPath: AbsolutePath,
            classLoader: ClassLoader = Thread.currentThread().contextClassLoader
        )

        // TODO
        //    fun registerFileSystemFolder(
        //        webBasePath: RelativePath,
        //        fileSystem: FileSystem,
        //        folderAbsolutePath: AbsolutePath
        //    )

        fun registerResource(
            fileWebPath: RelativePath,
            resource: Resource
        )
    }

    fun registerWebResources(context: Context)
}
