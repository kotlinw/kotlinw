package kotlinw.markdown.core.mp

import kotlinw.markdown.core.mp.parseMarkdownDocument
import kotlinw.markdown.core.mp.toMarkdownText
import kotlin.test.assertEquals

fun String.testBackConversion() {
    val originalMarkdownDocument = this
    val originalModel = originalMarkdownDocument.parseMarkdownDocument()
    val convertedMarkdownDocument = originalModel.toMarkdownText()
    val convertedModel = convertedMarkdownDocument.parseMarkdownDocument()
    assertEquals(originalModel, convertedModel)
}

