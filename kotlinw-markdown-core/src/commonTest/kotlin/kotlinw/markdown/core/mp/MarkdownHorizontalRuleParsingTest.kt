package kotlinw.markdown.core.mp

import kotlinw.markdown.core.mp.MarkdownDocumentElement.HorizontalRule
import kotlinw.markdown.core.mp.MarkdownDocumentElement.HorizontalRule.Type.Asterisk
import kotlinw.markdown.core.mp.MarkdownDocumentElement.HorizontalRule.Type.Hyphen
import kotlinw.markdown.core.mp.MarkdownDocumentElement.HorizontalRule.Type.Underscore
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
