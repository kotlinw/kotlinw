package xyz.kotlinw.util.ktor.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.prepareGet
import io.ktor.client.request.url
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes
import korlibs.io.file.VfsOpenMode
import kotlinw.util.stdlib.Url
import kotlinw.util.stdlib.io.FileLocation

data class FileDownloadProgressSnapshot(val fileSizeBytes: Long?, val downloadedBytes: Long)

suspend fun HttpClient.downloadFile(
    sourceUrl: Url,
    targetFile: FileLocation,
    requestBuilder: HttpRequestBuilder.() -> Unit = {},
    progressListener: (FileDownloadProgressSnapshot) -> Unit = {},
    downloadChunkSize: Int = 1024
) {
    return prepareGet {
        requestBuilder()
        url(sourceUrl.value)
    }.execute { httpResponse ->
        val httpStatusCode = httpResponse.status
        if (httpStatusCode.isSuccess()) {
            val fileLength = httpResponse.contentLength()
            targetFile.openUse(VfsOpenMode.CREATE_OR_TRUNCATE) {
                val channel = httpResponse.body<ByteReadChannel>()
                var downloadedBytes = 0L
                while (!channel.isClosedForRead) {
                    val packet = channel.readRemaining(downloadChunkSize.toLong())
                    while (!packet.isEmpty) {
                        val bytes = packet.readBytes()
                        write(bytes)

                        downloadedBytes += bytes.size
                        progressListener(FileDownloadProgressSnapshot(fileLength, downloadedBytes))
                    }
                }
            }
        } else {
            // TODO raise()
            throw RuntimeException("Failed to download '$sourceUrl': received HTTP status code $httpStatusCode.")
        }
    }
}
