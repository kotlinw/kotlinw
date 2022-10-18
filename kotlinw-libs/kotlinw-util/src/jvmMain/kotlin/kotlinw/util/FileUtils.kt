package kotlinw.util

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory

fun Path.isEmptyDirectory(): Boolean {
    check(isDirectory())
    return Files.list(this).findFirst().isPresent
}
