package kotlinw.util.stdlib.io

import okio.FileSystem
import okio.Path
import okio.buffer
import okio.use

data class FileLocation(val fileSystem: FileSystem, val path: Path)

fun FileLocation.readUtf8(): String =
    fileSystem.source(path).use {
        it.buffer().use {
            it.readUtf8()
        }
    }
