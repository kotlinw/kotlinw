package xyz.kotlinw.io

import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import java.io.File

fun FileLocation.toJavaFile(): File {
    require(fileSystem == SystemFileSystem)
    return path.toJavaPath().toFile()
}

fun File.toFileLocation() = FileLocation(Path(absolutePath), SystemFileSystem)
