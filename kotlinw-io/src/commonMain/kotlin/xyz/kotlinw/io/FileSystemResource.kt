package xyz.kotlinw.io

import kotlinx.io.RawSource
import kotlinx.io.files.FileSystem
import kotlinx.io.files.SystemFileSystem

data class FileSystemResource(val path: AbsolutePath, val fileSystem: FileSystem = SystemFileSystem) : Resource {

    private val fileSystemPath = path.toFileSystemPath()

    override val name: String get() = path.lastSegment

    override fun getContents(): RawSource = fileSystem.source(fileSystemPath)

    override fun exists(): Boolean = fileSystem.exists(fileSystemPath)

    override fun toString(): String {
        return "FileResource(path=$path, fileSystem=$fileSystem)"
    }
}
