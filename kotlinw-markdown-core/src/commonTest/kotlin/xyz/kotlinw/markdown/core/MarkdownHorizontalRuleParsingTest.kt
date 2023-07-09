package xyz.kotlinw.markdown.core

import xyz.kotlinw.markdown.core.MarkdownDocumentElement.HorizontalRule
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.HorizontalRule.Type.Asterisk
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.HorizontalRule.Type.Hyphen
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.HorizontalRule.Type.Underscore
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownHorizontalRuleParsingTest {
    @Test
    fun testHorizontalRule() {
        assertEquals(
            MarkdownDocumentModel(
                HorizontalRule(Hyphen),
                HorizontalRule(Underscore),
                HorizontalRule(Asterisk)
            ),
            """
                ---

                ___
                
                ***
            """.trimIndent().parseMarkdownDocument()
        )
    }
}
