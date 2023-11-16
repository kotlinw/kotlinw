package xyz.kotlinw.io

import kotlinx.io.Source
import kotlinx.io.buffered
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

    override fun toString(): String {
        return "FileResource(path=$path, fileSystem=$fileSystem)"
    }

    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun <T> useAsSource(block: suspend (Source) -> T): T =
        fileSystem.source(fileSystemPath).buffered().use {
            block(it)
        }

    override suspend fun exists(): Boolean =
        try {
            fileSystem.exists(fileSystemPath)
        } catch (e: Exception) {
            throw ResourceResolutionException(this, cause = e)
        }

    override suspend fun length(): Long? =
        try {
            val size = fileSystem.metadataOrNull(fileSystemPath)?.size
            if (size == -1L) null else size
        } catch (e: Exception) {
            throw ResourceResolutionException(this, cause = e)
        }
}
