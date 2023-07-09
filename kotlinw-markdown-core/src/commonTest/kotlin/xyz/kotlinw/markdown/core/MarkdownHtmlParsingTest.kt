package xyz.kotlinw.markdown.core

import xyz.kotlinw.markdown.core.MarkdownDocumentElement.HtmlBlock
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownHtmlParsingTest {
    @Test
    fun testHtml() {
        val sourceDocument = """
            <div>
                <span>some text</span>
            </div>
        """.trimIndent()
        assertEquals(
            MarkdownDocumentModel(
                HtmlBlock(
                    """
                        <div>
                            <span>some text</span>
                        </div>
                    """.trimIndent()
                )
            ),
            sourceDocument.parseMarkdownDocument()
        )
    }
}
