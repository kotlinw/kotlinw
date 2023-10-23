package xyz.kotlinw.io

import java.io.File
import java.nio.file.Path

val FileSystemResource.javaPath: Path get() = fileSystemPath.toJavaPath()

fun FileSystemResource.toJavaFile(): File = javaPath.toFile()
