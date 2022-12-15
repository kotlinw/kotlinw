package kotlinw.util.stdlib

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory

fun Path.isEmptyDirectory(): Boolean {
    check(isDirectory())
    return Files.list(this).findFirst().isEmpty
}
