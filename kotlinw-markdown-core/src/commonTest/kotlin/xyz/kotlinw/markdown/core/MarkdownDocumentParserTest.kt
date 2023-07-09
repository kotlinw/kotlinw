package xyz.kotlinw.markdown.core

import xyz.kotlinw.markdown.core.MarkdownDocumentModel
import xyz.kotlinw.markdown.core.parseMarkdownDocument
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownDocumentParserTest {
    @Test
    fun testEmptyDocument() {
        assertEquals(MarkdownDocumentModel(emptyList()), "".parseMarkdownDocument())
    }
}
