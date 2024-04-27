package xyz.kotlinw.util.ktor.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.prepareGet
import io.ktor.client.request.url
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.util.date.getTimeMillis
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinw.logging.api.Logger
import kotlinw.util.stdlib.Url
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import xyz.kotlinw.io.FileLocation
import xyz.kotlinw.io.bufferedSink

data class FileDownloadProgressSnapshot(val sourceUrl: Url, val fileSizeBytes: Long?, val downloadedBytes: Long)

class LoggingProgressListener(private val logger: Logger) {

    private var lastProgressReportMillis = 0L

    fun logProgress(progressSnapshot: FileDownloadProgressSnapshot) {
        val now = getTimeMillis()
        if (now - lastProgressReportMillis >= 1000) {
            lastProgressReportMillis = now
            logger.info {
                "Downloading " / progressSnapshot.sourceUrl / ": " / progressSnapshot.downloadedBytes / " / " / (progressSnapshot.fileSizeBytes
                    ?: "?")
            }
        }
    }
}

@OptIn(ExperimentalStdlibApi::class)
suspend fun HttpClient.downloadFile(
    sourceUrl: Url,
    targetFile: FileLocation,
    requestBuilder: HttpRequestBuilder.() -> Unit = {},
    progressListener: (FileDownloadProgressSnapshot) -> Unit = {},
    downloadChunkSize: Int = 8 * 1024,
    progressReportInterval: Duration = 1.seconds
) =
    prepareGet {
        requestBuilder()
        url(sourceUrl.value)
    }.execute { httpResponse ->
        val httpStatusCode = httpResponse.status
        if (httpStatusCode.isSuccess()) {
            val fileLength = httpResponse.contentLength()
            targetFile.bufferedSink().use { sink ->
                val channel = httpResponse.body<ByteReadChannel>()
                var downloadedBytes = 0L
                var lastProgressReport = Instant.DISTANT_PAST

                fun reportProgress() = progressListener(FileDownloadProgressSnapshot(sourceUrl, fileLength, downloadedBytes))

                reportProgress()

                while (!channel.isClosedForRead) {
                    val packet = channel.readRemaining(downloadChunkSize.toLong())
                    while (!packet.isEmpty) {
                        val bytes = packet.readBytes()

                        sink.write(bytes)

                        downloadedBytes += bytes.size

                        val now = Clock.System.now()
                        if (lastProgressReport + progressReportInterval < now) {
                            lastProgressReport = now
                            reportProgress()
                        }
                    }
                }

                reportProgress()
            }
        } else {
            // TODO raise()
            throw RuntimeException("Failed to download '$sourceUrl': received HTTP status code $httpStatusCode.")
        }
    }
