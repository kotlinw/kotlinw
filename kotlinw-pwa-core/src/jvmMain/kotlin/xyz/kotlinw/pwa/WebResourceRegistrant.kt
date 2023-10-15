package xyz.kotlinw.pwa

import xyz.kotlinw.io.AbsolutePath
import xyz.kotlinw.io.RelativePath

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
    }

    fun registerWebResources(context: Context)
}
