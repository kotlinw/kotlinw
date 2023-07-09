package xyz.kotlinw.markdown.core

import xyz.kotlinw.markdown.core.MarkdownDocumentElement.CodeBlock
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.Heading
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.InlineCode
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.InlineLink
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.LinkDefinition
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.Paragraph
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.Text
import kotlin.test.Test
import kotlin.test.assertEquals

class KotlinReferenceDocumentationParsingTest {
    @Test
    fun testSectionBasicSyntax() {
        assertEquals(
            MarkdownDocumentModel(
                MarkdownMetadata.of("title" to "Basic syntax"),
                LinkDefinition("//", "#", "(title: Basic syntax)"),
                Paragraph("This is a collection of basic syntax elements with examples. At the end of every section, you'll find a link to a detailed description of the related topic."),
                Paragraph(
                    Text("You can also learn all the Kotlin essentials with the free "),
                    InlineLink(
                        "https://hyperskill.org/join/fromdocstoJetSalesStat?redirect=true&next=/tracks/18",
                        "Kotlin Basics track"
                    ),
                    Text(" on JetBrains Academy.")
                ),
                Heading(
                    2, "Package definition and imports",
                    Paragraph("Package specification should be at the top of the source file."),
                    CodeBlock(
                        """
                    package my.demo

                    import kotlin.text.*

                    // ...
                    """.trimIndent(),
                        "kotlin"
                    ),
                    Paragraph("It is not required to match directories and packages: source files can be placed arbitrarily in the file system."),
                    Paragraph(
                        Text("See "),
                        InlineLink("packages.md", "Packages"),
                        Text(".")
                    )
                ),
                Heading(
                    2, "Program entry point",
                    Paragraph(
                        Text("An entry point of a Kotlin application is the "),
                        InlineCode("main"),
                        Text(" function.")
                    ),
                    CodeBlock(
                        """
                    fun main() {
                        println("Hello world!")
                    }
                    """.trimIndent(),
                        "kotlin",
                        MarkdownMetadata("kotlin-runnable" to "true", "kotlin-min-compiler-version" to "1.3")
                    ),
                    Paragraph(
                        Text("Another form of "),
                        InlineCode("main"),
                        Text(" accepts a variable number of "),
                        InlineCode("String"),
                        Text(" arguments.")
                    ),
                    CodeBlock(
                        """
                        fun main(args: Array<String>) {
                            println(args.contentToString())
                        }
                    """.trimIndent(),
                        "kotlin",
                        MarkdownMetadata("kotlin-runnable" to "true", "kotlin-min-compiler-version" to "1.3")
                    )
                ),
                Heading(
                    2, "Print to the standard output",
                    Paragraph(
                        InlineCode("print"),
                        Text(" prints its argument to the standard output.")
                    ),
                    CodeBlock(
                        """
                    fun main() {
                    //sampleStart
                        print("Hello ")
                        print("world!")
                    //sampleEnd
                    }
                    """.trimIndent(),
                        "kotlin",
                        MarkdownMetadata("kotlin-runnable" to "true", "kotlin-min-compiler-version" to "1.3")
                    ),
                    Paragraph(
                        InlineCode("println"),
                        Text(" prints its arguments and adds a line break, so that the next thing you print appears on the next line.")
                    ),
                    CodeBlock(
                        """
                    fun main() {
                    //sampleStart
                        println("Hello world!")
                        println(42)
                    //sampleEnd
                    }
                    """.trimIndent(),
                        "kotlin",
                        MarkdownMetadata("kotlin-runnable" to "true", "kotlin-min-compiler-version" to "1.3")
                    ),
                    Paragraph("..."),
                    Paragraph(
                        Text("See "),
                        InlineLink("classes.md", "Classes"),
                        Text(" and "),
                        InlineLink("typecasts.md", "Type casts"),
                        Text(".")
                    )
                )
            ),
            """
[//]: # (title: Basic syntax)

This is a collection of basic syntax elements with examples. At the end of every section, you'll find a link to
a detailed description of the related topic.

You can also learn all the Kotlin essentials with the free [Kotlin Basics track](https://hyperskill.org/join/fromdocstoJetSalesStat?redirect=true&next=/tracks/18)
on JetBrains Academy.

## Package definition and imports

Package specification should be at the top of the source file.

```kotlin
package my.demo

import kotlin.text.*

// ...
```

It is not required to match directories and packages: source files can be placed arbitrarily in the file system.

See [Packages](packages.md).

## Program entry point

An entry point of a Kotlin application is the `main` function.

```kotlin
fun main() {
    println("Hello world!")
}
```
{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}

Another form of `main` accepts a variable number of `String` arguments.

```kotlin
fun main(args: Array<String>) {
    println(args.contentToString())
}
```
{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}

## Print to the standard output

`print` prints its argument to the standard output.

```kotlin
fun main() {
//sampleStart
    print("Hello ")
    print("world!")
//sampleEnd
}
```
{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}

`println` prints its arguments and adds a line break, so that the next thing you print appears on the next line.

```kotlin
fun main() {
//sampleStart
    println("Hello world!")
    println(42)
//sampleEnd
}
```
{kotlin-runnable="true" kotlin-min-compiler-version="1.3"}

...

See [Classes](classes.md) and [Type casts](typecasts.md).
            """.trimIndent().parseMarkdownDocument()
        )
    }
}
