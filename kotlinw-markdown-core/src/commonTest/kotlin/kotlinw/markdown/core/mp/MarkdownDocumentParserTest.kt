package kotlinw.markdown.core.mp

import kotlinw.markdown.core.mp.MarkdownDocumentModel
import kotlinw.markdown.core.mp.parseMarkdownDocument
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownDocumentParserTest {
    @Test
    fun testEmptyDocument() {
        assertEquals(MarkdownDocumentModel(emptyList()), "".parseMarkdownDocument())
    }
}
