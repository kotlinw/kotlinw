package xyz.kotlinw.markdown.core

import xyz.kotlinw.markdown.core.MarkdownDocumentElement.BlockElement
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.BlockQuote
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.CodeBlock
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.HardLineBreak
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.Heading
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.HorizontalRule
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.HtmlBlock
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.HtmlTag
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.Image
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.InlineCode
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.InlineElement
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.InlineLink
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.LinkDefinition
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.ListItem
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.ListItems
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.Paragraph
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.Table
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.Table.Cell
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.Text
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.TextSpan

fun MarkdownDocumentModel.accept(visitor: MarkdownDocumentModelVisitor) {
    visitor.visitDocument(this)
}

interface MarkdownDocumentModelVisitor {
    fun visitDocument(document: MarkdownDocumentModel) {
        document.childElements.forEach {
            visitBlockElement(it)
        }
    }

    fun visitBlockElement(blockElement: BlockElement) {
        when (blockElement) {
            is BlockQuote -> visitBlockQuote(blockElement)
            is CodeBlock -> visitCodeBlock(blockElement)
            is Heading -> visitHeading(blockElement)
            is HorizontalRule -> visitHorizontalRule(blockElement)
            is HtmlBlock -> visitHtmlBlock(blockElement)
            is LinkDefinition -> visitLinkDefinition(blockElement)
            is ListItem -> visitListItem(blockElement)
            is ListItems -> visitListItems(blockElement)
            is Paragraph -> visitParagraph(blockElement)
            is Table -> visitTable(blockElement)
        }
    }

    fun visitTable(table: Table) {
        visitTableHeaderRow(table.headers)

        table.rows.forEachIndexed { rowIndex, rowCells ->
            visitTableBodyRow(rowCells, rowIndex)
        }
    }

    fun visitTableBodyRow(rowCells: List<Cell>, rowIndex: Int) {
        rowCells.forEachIndexed { columnIndex, bodyCell ->
            visitTableBodyCell(bodyCell, rowIndex, columnIndex)
        }
    }

    fun visitTableBodyCell(bodyCell: Cell, rowIndex: Int, columnIndex: Int) {
    }

    fun visitTableHeaderRow(headerCells: List<Cell>) {
        headerCells.forEachIndexed { columnIndex, cell ->
            visitTableHeaderCell(cell, columnIndex)
        }
    }

    fun visitTableHeaderCell(headerCell: Cell, columnIndex: Int) {
    }

    fun visitParagraph(paragraph: Paragraph) {
        paragraph.childElements.forEach {
            visitInlineElement(it)
        }
    }

    fun visitListItems(listItems: ListItems) {
        listItems.items.forEach {
            visitListItem(it)
        }
    }

    fun visitListItem(listItem: ListItem) {
        listItem.contents.forEach {
            visitBlockElement(it)
        }
    }

    fun visitLinkDefinition(linkDefinition: LinkDefinition) {
    }

    fun visitHtmlBlock(htmlBlock: HtmlBlock) {
    }

    fun visitHorizontalRule(horizontalRule: HorizontalRule) {
    }

    fun visitHeading(heading: Heading) {
        heading.heading.forEach {
            visitInlineElement(it)
        }

        heading.contents.forEach {
            visitBlockElement(it)
        }
    }

    fun visitCodeBlock(codeBlock: CodeBlock) {
    }

    fun visitBlockQuote(blockQuote: BlockQuote) {
        blockQuote.childElements.forEach {
            visitBlockElement(it)
        }
    }

    fun visitInlineElement(inlineElement: InlineElement) {
        when (inlineElement) {
            is HardLineBreak -> visitHardLineBreak(inlineElement)
            is HtmlTag -> visitHtmlTag(inlineElement)
            is Image -> visitImage(inlineElement)
            is InlineCode -> visitInlineCode(inlineElement)
            is InlineLink -> visitInlineLink(inlineElement)
            is Text -> visitText(inlineElement)
            is TextSpan -> visitTextSpan(inlineElement)
        }
    }

    fun visitTextSpan(textSpan: TextSpan) {
        textSpan.childElements.forEach {
            visitInlineElement(it)
        }
    }

    fun visitText(text: Text) {
    }

    fun visitInlineLink(inlineLink: InlineLink) {
        inlineLink.textElements.forEach {
            visitInlineElement(it)
        }
    }

    fun visitInlineCode(inlineCode: InlineCode) {
    }

    fun visitImage(image: Image) {
        image.descriptionElements.forEach {
            visitInlineElement(it)
        }
    }

    fun visitHtmlTag(htmlTag: HtmlTag) {
    }

    fun visitHardLineBreak(hardLineBreak: HardLineBreak) {
    }
}
