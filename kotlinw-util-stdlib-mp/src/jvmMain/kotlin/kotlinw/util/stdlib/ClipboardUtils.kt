package kotlinw.util.stdlib

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import java.awt.HeadlessException
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

object ClipboardUtils {

    enum class CopyToClipboardResult {
        Ok,
        NotSupportedInHeadlessMode
    }

    fun String.copyToClipboard(): Result<CopyToClipboardResult, Throwable> =
        runCatching {
            try {
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                clipboard.setContents(
                    StringSelection(this)
                ) { _, _ -> }
                CopyToClipboardResult.Ok
            } catch (e: HeadlessException) {
                CopyToClipboardResult.NotSupportedInHeadlessMode
            }
        }
}
