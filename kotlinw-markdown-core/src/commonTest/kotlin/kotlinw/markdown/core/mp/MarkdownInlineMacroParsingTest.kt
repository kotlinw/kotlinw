package kotlinw.markdown.core.mp

import kotlinw.markdown.core.mp.MarkdownDocumentElement.InlineLink
import kotlinw.markdown.core.mp.MarkdownDocumentElement.Paragraph
import kotlinw.markdown.core.mp.MarkdownDocumentElement.Text
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownInlineMacroParsingTest {
    @Test
    fun testInlineMacroParsing() {
        assertEquals(
            MarkdownDocumentModel(
                Paragraph(
                    InlineLink("/some/link", Text(invalidMacroPlaceholder))
                )
            ),
            "[\${some-invalid-macro-name}](/some/link)".parseMarkdownDocument()
        )
    }
}
