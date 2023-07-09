package kotlinw.markdown.core.mp

import kotlinw.markdown.core.mp.MarkdownDocumentElement.CodeBlock
import kotlinw.markdown.core.mp.MarkdownDocumentElement.InlineCode
import kotlinw.markdown.core.mp.MarkdownDocumentElement.Paragraph
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownCodeParsingTest {
    @Test
    fun testInlineCode() {
        val sourceDocument = "`result == 42`"
        assertEquals(
            MarkdownDocumentModel(
                Paragraph(
                    InlineCode("result == 42")
                )
            ),
            sourceDocument.parseMarkdownDocument()
        )
    }

    @Test
    fun testCodeBlock() {
        val sourceDocument =
            """
                |    class A {
                |    }
            """.trimMargin()
        assertEquals(
            MarkdownDocumentModel(
                CodeBlock(
                    """
                        class A {
                        }
                    """.trimIndent()
                )
            ),
            sourceDocument.parseMarkdownDocument()
        )
    }

    @Test
    fun testCodeBlockWithIndents() {
        val sourceDocument =
            """
                |    class A {
                |        class B
                |        
                |        fun f() =
                |          ""
                |    }
            """.trimMargin()
        assertEquals(
            MarkdownDocumentModel(
                CodeBlock(
                    """
                        class A {
                            class B
                            
                            fun f() =
                              ""
                        }
                    """.trimIndent()
                )
            ),
            sourceDocument.parseMarkdownDocument()
        )
    }

    @Test
    fun testCodeFence() {
        val sourceDocument =
            """
                ```
                class A {
                }
                ```
            """.trimIndent()
        assertEquals(
            MarkdownDocumentModel(
                CodeBlock(
                    """
                        class A {
                        }
                    """.trimIndent()
                )
            ),
            sourceDocument.parseMarkdownDocument()
        )
    }

    @Test
    fun testComplexCodeFence() {
        val sourceDocument =
            """
                ```kotlin
                import B
                
                class A {
                    var b: B
                }
                ```
            """.trimIndent()
        assertEquals(
            MarkdownDocumentModel(
                CodeBlock(
                    """
                        import B
                        
                        class A {
                            var b: B
                        }
                    """.trimIndent(), "kotlin"
                )
            ),
            sourceDocument.parseMarkdownDocument()
        )
    }
}
