package xyz.kotlinw.markdown.core

import xyz.kotlinw.markdown.core.parseMarkdownDocument
import xyz.kotlinw.markdown.core.toMarkdownText
import kotlin.test.assertEquals

fun String.testBackConversion() {
    val originalMarkdownDocument = this
    val originalModel = originalMarkdownDocument.parseMarkdownDocument()
    val convertedMarkdownDocument = originalModel.toMarkdownText()
    val convertedModel = convertedMarkdownDocument.parseMarkdownDocument()
    assertEquals(originalModel, convertedModel)
}

