package xyz.kotlinw.io

import kotlinx.io.RawSource
import kotlinx.io.files.FileSystem
import kotlinx.io.files.SystemFileSystem

data class FileSystemResource(val path: AbsolutePath, val fileSystem: FileSystem = SystemFileSystem) : Resource {

    private val fileSystemPath = path.toFileSystemPath()

    override val name: String get() = path.lastSegment

    constructor(fileLocation: FileLocation) : this(
        (fileLocation.path.toNormalizedPath() as? AbsolutePath)
            ?: throw IllegalArgumentException("Absolute path expected: ${fileLocation.path}"),
        fileLocation.fileSystem
    )

    override fun getContents(): RawSource = fileSystem.source(fileSystemPath)

    override fun exists(): Boolean = fileSystem.exists(fileSystemPath)

    override fun length(): Long? {
        val size = fileSystem.metadataOrNull(fileSystemPath)?.size
        return if (size == -1L) null else size
    }

    override fun toString(): String {
        return "FileResource(path=$path, fileSystem=$fileSystem)"
    }
}