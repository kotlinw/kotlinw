package xyz.kotlinw.io

import kotlinx.io.buffered
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.readString
import kotlinx.io.writeString

data class FileLocation(val fileSystem: FileSystem, val path: Path)

@Deprecated(
    message = "Not type-safe.",
    replaceWith = ReplaceWith("get(RelativePath(relativePath))", imports = ["xyz.kotlinw.io.RelativePath"])
)
operator fun FileLocation.get(relativePath: String) = FileLocation(fileSystem, Path(path, relativePath))

operator fun FileLocation.get(relativePath: RelativePath) = FileLocation(fileSystem, Path(path, relativePath.value))

val FileLocation.parent get() = path.parent?.let { FileLocation(fileSystem, it) }

fun FileLocation.exists() = fileSystem.exists(path)

fun FileLocation.isDirectory() = fileSystem.metadataOrNull(path)?.isDirectory
    ?: throw IllegalArgumentException("File metadata is not available: $this")

fun FileLocation.isRegularFile() = fileSystem.metadataOrNull(path)?.isRegularFile
    ?: throw IllegalArgumentException("File metadata is not available: $this")

fun FileLocation.mkdirs(): Boolean {
    fileSystem.createDirectories(path)
    return exists()
}

fun FileLocation.delete() = fileSystem.delete(path)

fun FileLocation.mkdir(): Boolean {
    return mkdirs() // TODO check if parent directory exists
}

fun FileLocation.source() = fileSystem.source(path)

fun FileLocation.bufferedSource() = source().buffered()

fun FileLocation.sink() = fileSystem.sink(path)

fun FileLocation.bufferedSink() = sink().buffered()

fun FileLocation.readString() = bufferedSource().readString()

fun FileLocation.writeString(string: String) = bufferedSink().writeString(string)
