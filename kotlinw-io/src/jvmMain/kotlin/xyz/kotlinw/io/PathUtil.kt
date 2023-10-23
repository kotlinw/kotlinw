package xyz.kotlinw.io

import kotlin.io.path.pathString
import kotlinx.io.files.Path
import java.nio.file.Paths

fun Path.toJavaPath(): java.nio.file.Path = Paths.get(toString())

fun java.nio.file.Path.toKotlinPath(): Path = Path(pathString)
