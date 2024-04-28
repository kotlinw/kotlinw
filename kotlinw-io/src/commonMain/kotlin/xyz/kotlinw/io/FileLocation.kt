package xyz.kotlinw.io

import kotlinx.io.buffered
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString

data class FileLocation(val path: Path, val fileSystem: FileSystem = SystemFileSystem)

@Deprecated(
    message = "Not type-safe.",
    replaceWith = ReplaceWith("get(RelativePath(relativePath))", imports = ["xyz.kotlinw.io.RelativePath"])
)
operator fun FileLocation.get(relativePath: String) = FileLocation(Path(path, relativePath), fileSystem)

operator fun FileLocation.get(relativePath: RelativePath) = FileLocation(Path(path, relativePath.value), fileSystem)

val FileLocation.parent get() = path.parent?.let { FileLocation(it, fileSystem) }

fun FileLocation.exists() = fileSystem.exists(path)

fun FileLocation.isDirectory() = fileSystem.metadataOrNull(path)?.isDirectory
    ?: throw IllegalArgumentException("File metadata is not available: $this")

fun FileLocation.isRegularFile() = fileSystem.metadataOrNull(path)?.isRegularFile
    ?: throw IllegalArgumentException("File metadata is not available: $this")

fun FileLocation.mkdirs(): Boolean {
    fileSystem.createDirectories(path)
    return exists()
}

fun FileLocation.delete(mustExist: Boolean = true) = fileSystem.delete(path, mustExist)

fun FileLocation.mkdir(): Boolean {
    return mkdirs() // TODO check if parent directory exists
}

fun FileLocation.source() = fileSystem.source(path)

fun FileLocation.bufferedSource() = source().buffered()

fun FileLocation.sink() = fileSystem.sink(path)

fun FileLocation.bufferedSink() = sink().buffered()

fun FileLocation.readString(): String = bufferedSource().use { it.readString() }

fun FileLocation.writeString(string: String) {
    bufferedSink().use {
        it.writeString(string)
    }
}
