package xyz.kotlinw.pwa

import xyz.kotlinw.io.AbsolutePath
import xyz.kotlinw.io.RelativePath
import xyz.kotlinw.io.Resource

interface WebResourceRegistry {

    fun registerClasspathFolder(
        folderWebBasePath: RelativePath,
        classLoader: ClassLoader,
        classpathFolderPath: AbsolutePath
    )

    // TODO
//    fun registerFileSystemFolder(
//        webBasePath: RelativePath,
//        fileSystem: FileSystem,
//        folderAbsolutePath: AbsolutePath
//    )

    fun resolveWebUrl(resource: Resource): RelativePath?
}
