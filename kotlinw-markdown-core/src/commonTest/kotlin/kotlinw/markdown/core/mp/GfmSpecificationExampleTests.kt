package kotlinw.markdown.core.mp

import kotlin.test.Test
import kotlin.test.assertEquals

class GfmSpecificationExampleTests {
    /**
     * See: [example-35](https://github.github.com/gfm/#example-35)
     */
    @Test
    fun testExample35() {
        assertEquals(
            """
                ## foo
                
            """.trimIndent(),
            """\## foo""".parseMarkdownDocument().toMarkdownText()
        )
    }
}
