package xyz.kotlinw.io

import kotlinx.io.RawSource
import kotlinx.io.files.FileNotFoundException

class FileSystemResourceResolver: ResourceResolver<FileSystemResource> {

    override fun open(resource: FileSystemResource): RawSource =
        try {
            resource.fileSystem.source(resource.fileSystemPath)
        } catch (e: FileNotFoundException) {
            throw ResourceResolutionException(resource, "File not found.", e)
        } catch (e: Exception) {
            throw ResourceResolutionException(resource, cause = e)
        }

    override fun exists(resource: FileSystemResource): Boolean =
        try {
            resource.fileSystem.exists(resource.fileSystemPath)
        } catch (e: Exception) {
            throw ResourceResolutionException(resource, cause = e)
        }

    override fun length(resource: FileSystemResource): Long? =
        try {
            val size = resource.fileSystem.metadataOrNull(resource.fileSystemPath)?.size
            if (size == -1L) null else size
        } catch (e: Exception) {
            throw ResourceResolutionException(resource, cause = e)
        }

}