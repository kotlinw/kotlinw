package kotlinw.util

import mu.KotlinLogging
import java.awt.HeadlessException
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

private val logger = KotlinLogging.logger {}

object ClipboardUtils {
    fun String.copyToClipboard(): Boolean =
        try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(
                StringSelection(this)
            ) { _, _ -> }
            logger.info("Copied to clipboard: $this")
            true
        } catch (e: HeadlessException) {
            logger.warn("Cannot copy to clipboard in headless mode, add -Djava.awt.headless=false to the JVM arguments to allow access to the clipboard. text='$this'")
            false
        } catch (e: Exception) {
            logger.warn("Cannot copy to clipboard: '$this'", e)
            false
        }
}
