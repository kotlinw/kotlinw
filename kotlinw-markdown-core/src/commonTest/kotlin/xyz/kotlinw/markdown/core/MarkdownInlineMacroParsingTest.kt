package xyz.kotlinw.markdown.core

import xyz.kotlinw.markdown.core.MarkdownDocumentElement.InlineLink
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.Paragraph
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.Text
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
