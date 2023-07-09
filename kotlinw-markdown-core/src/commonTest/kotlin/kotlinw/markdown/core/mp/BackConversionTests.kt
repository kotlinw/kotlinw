package kotlinw.markdown.core.mp

import kotlin.test.Test

class BackConversionTests {
    @Test
    fun testSimpleText() {
        "some text".testBackConversion()
        """
            some text
            
        """.trimIndent().testBackConversion()
        """

            some text
            
        """.trimIndent().testBackConversion()
    }

    @Test
    fun testMultiLineText() {
        """
            First line,
            second line,
            last line.
            
        """
            .trimIndent().testBackConversion()
    }

    @Test
    fun testHardLineBreak() {
        """
            Hard  
            line break.
            
        """
            .trimIndent().testBackConversion()
    }

    @Test
    fun testTable() {
        """
            | Library    | Details                                                            |
            |------------|--------------------------------------------------------------------| 
            | Ktor       | [Docs](https://ktor.io/docs/client.html).                          | 
            | DateTime   | [Docs](https://github.com/Kotlin/kotlinx-datetime#readme).         |
            | SQLDelight | Third-party library. [Docs](https://cashapp.github.io/sqldelight/).|
        """.trimIndent().testBackConversion()
    }
}
