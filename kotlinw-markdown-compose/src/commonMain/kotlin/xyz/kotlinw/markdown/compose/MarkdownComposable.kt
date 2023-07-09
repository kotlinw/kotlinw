package xyz.kotlinw.markdown.compose

import androidx.compose.runtime.Composable
import xyz.kotlinw.markdown.core.MarkdownDocumentModel

@Composable
expect fun MarkdownDocument(markdownDocument: MarkdownDocumentModel)
