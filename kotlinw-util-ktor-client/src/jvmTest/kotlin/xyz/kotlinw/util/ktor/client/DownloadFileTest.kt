package xyz.kotlinw.util.ktor.client

import io.ktor.client.HttpClient
import kotlin.io.path.createTempFile
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlinw.util.stdlib.Url
import kotlinw.util.stdlib.formatByteSize
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Clock.System
import kotlinx.datetime.Instant
import xyz.kotlinw.io.FileLocation
import xyz.kotlinw.io.toKotlinPath


class DownloadFileTest {

    // Test files:
    // - https://www.thinkbroadband.com/download
    // - https://testfile.org/all-sizes/
    @Test
    fun testDownloadFile() = runBlocking {
        val targetFile = createTempFile()
        HttpClient().downloadFile(
            sourceUrl = Url("http://ipv4.download.thinkbroadband.com/1GB.zip"),
            targetFile = FileLocation(targetFile.toKotlinPath()),
            progressListener = { println(System.now().toString() + " " + it.downloadedBytes.formatByteSize() + " " + it.downloadSpeedAverageBytesPerSecond?.formatByteSize() + "/s") },
            // progressReportInterval = 0.5.seconds,
            // throttleDownloadSpeed = 50 * 1024,
            // chunkSize = 16 * 1024
        )
    }
}
