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
import kotlin.time.TimeSource
import kotlinw.logging.api.Logger
import kotlinw.util.stdlib.IncrementalAverageHolder.MutableIncrementalAverageHolder
import kotlinw.util.stdlib.Url
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import xyz.kotlinw.io.FileLocation
import xyz.kotlinw.io.bufferedSink

data class FileDownloadProgressSnapshot(
    val sourceUrl: Url,
    val fileSizeBytes: Long?,
    val downloadedBytes: Long,
    val downloadSpeedAverageBytesPerSecond: Int?
)

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
    chunkSize: Int = 16 * 1024,
    progressReportInterval: Duration = 1.seconds,
    fileSizeLimit: Long? = null,
    throttleDownloadSpeed: Int? = null
) =
    prepareGet {
        requestBuilder()
        url(sourceUrl.value)
    }.execute { httpResponse ->
        val httpStatusCode = httpResponse.status
        if (httpStatusCode.isSuccess()) {
            val fileLength = httpResponse.contentLength()

            if (fileSizeLimit != null && fileLength != null && fileLength > fileSizeLimit) {
                throw RuntimeException("File size limit exceeded: $fileSizeLimit") // TODO raise
            }

            targetFile.bufferedSink().use { sink ->
                val channel = httpResponse.body<ByteReadChannel>()
                var downloadedBytes = 0L
                var lastProgressReport = Instant.DISTANT_PAST
                val timeSource = TimeSource.Monotonic
                var downloadedBytesSinceLastProgressReport = 0L

                fun reportProgress(now: Instant = Clock.System.now()) {
                    val downloadSpeed =
                        try {
                            val downloadDuration = now - lastProgressReport
                            downloadedBytesSinceLastProgressReport.toDouble() / downloadDuration.inWholeMicroseconds.toDouble() * 1_000_000.0
                        } catch (e: Exception) {
                            null
                        }

                    lastProgressReport = now
                    downloadedBytesSinceLastProgressReport = 0

                    progressListener(
                        FileDownloadProgressSnapshot(
                            sourceUrl,
                            fileLength,
                            downloadedBytes,
                            downloadSpeed?.toInt()
                        )
                    )
                }

                reportProgress()

                while (!channel.isClosedForRead) {
                    val packetDownloadStart = timeSource.markNow()
                    val packet = channel.readRemaining(chunkSize.toLong())

                    while (!packet.isEmpty) {
                        val bytes = packet.readBytes()
                        val packetDownloadRealDuration = try {
                            packetDownloadStart.elapsedNow()
                        } catch (e: Exception) {
                            null
                        }

                        sink.write(bytes)

                        val packetSize = bytes.size
                        downloadedBytes += packetSize

                        if (fileSizeLimit != null && downloadedBytes > fileSizeLimit) {
                            throw RuntimeException("File size limit exceeded: $fileSizeLimit") // TODO raise
                        }

                        if (throttleDownloadSpeed != null && packetDownloadRealDuration != null) {
                            val packetDownloadDurationAtMaximumAllowedSpeed =
                                (packetSize.toDouble() / throttleDownloadSpeed.toDouble()).seconds
                            if (packetDownloadRealDuration < packetDownloadDurationAtMaximumAllowedSpeed) {
                                delay(packetDownloadDurationAtMaximumAllowedSpeed - packetDownloadRealDuration)
                            }
                        }

                        downloadedBytesSinceLastProgressReport += packetSize

                        val now = Clock.System.now()
                        if (lastProgressReport + progressReportInterval < now) {
                            reportProgress(now)
                        }
                    }
                }

                reportProgress()
            }
        } else {
            throw RuntimeException("Failed to download '$sourceUrl': received HTTP status code $httpStatusCode.") // TODO raise()
        }
    }
