package xyz.kotlinw.io

import kotlinx.io.files.SystemFileSystem
import java.io.File

fun FileLocation.toJavaFile(): File {
    require(fileSystem == SystemFileSystem)
    return File(path.toString())
}
