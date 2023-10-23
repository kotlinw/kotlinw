package xyz.kotlinw.io

import kotlinx.io.RawSource
import kotlinx.io.files.FileNotFoundException
import kotlinx.io.files.FileSystem
import kotlinx.io.files.SystemFileSystem

data class FileSystemResource(val path: AbsolutePath, val fileSystem: FileSystem = SystemFileSystem) : Resource {

    init {
        require(path.value != NormalizedPath.ROOT_PATH_STRING)
    }

    val fileSystemPath = path.toFileSystemPath()

    override val name: String get() = path.lastSegment

    constructor(fileLocation: FileLocation) :
            this(
                (fileLocation.path.toNormalizedPath() as? AbsolutePath)
                    ?: throw IllegalArgumentException("Absolute path expected: ${fileLocation.path}"),
                fileLocation.fileSystem
            )

    override fun open(): RawSource =
        try {
            fileSystem.source(fileSystemPath)
        } catch (e: FileNotFoundException) {
            throw ResourceResolutionException(this, "File not found.", e)
        } catch (e: Exception) {
            throw ResourceResolutionException(this, cause = e)
        }

    override fun exists(): Boolean =
        try {
            fileSystem.exists(fileSystemPath)
        } catch (e: Exception) {
            throw ResourceResolutionException(this, cause = e)
        }

    override fun length(): Long? =
        try {
            val size = fileSystem.metadataOrNull(fileSystemPath)?.size
            if (size == -1L) null else size
        } catch (e: Exception) {
            throw ResourceResolutionException(this, cause = e)
        }

    override fun toString(): String {
        return "FileResource(path=$path, fileSystem=$fileSystem)"
    }
}
