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

fun MarkdownDocumentModel.transform(transformer: MarkdownDocumentModelTransformer): MarkdownDocumentModel =
    transformer.transformDocument(this)

interface MarkdownDocumentModelTransformer {
    fun transformDocument(originalDocument: MarkdownDocumentModel): MarkdownDocumentModel =
        MarkdownDocumentModel(
            originalDocument.childElements.mapNotNull {
                transformBlockElement(it)
            },
            originalDocument.metadata
        )

    fun transformBlockElement(originalBlockElement: BlockElement): BlockElement? =
        when (originalBlockElement) {
            is BlockQuote -> transformBlockQuote(originalBlockElement)
            is CodeBlock -> transformCodeBlock(originalBlockElement)
            is Heading -> transformHeading(originalBlockElement)
            is HorizontalRule -> transformHorizontalRule(originalBlockElement)
            is HtmlBlock -> transformHtmlBlock(originalBlockElement)
            is LinkDefinition -> transformLinkDefinition(originalBlockElement)
            is ListItem -> transformListItem(originalBlockElement)
            is ListItems -> transformListItems(originalBlockElement)
            is Paragraph -> transformParagraph(originalBlockElement)
            is Table -> transformTable(originalBlockElement)
        }

    fun transformTable(originalTable: Table): BlockElement? =
        Table(
            transformTableHeaderRow(originalTable.headers) ?: emptyList(),
            originalTable.rows.mapIndexedNotNull { rowIndex, rowCells ->
                transformTableBodyRow(rowCells, rowIndex)
            },
            originalTable.metadata
        )

    fun transformTableBodyRow(rowCells: List<Cell>, rowIndex: Int): List<Cell>? =
        rowCells.mapIndexedNotNull { columnIndex, bodyCell ->
            transformTableBodyCell(bodyCell, rowIndex, columnIndex)
        }

    fun transformTableBodyCell(bodyCell: Cell, rowIndex: Int, columnIndex: Int): Cell? =
        bodyCell

    fun transformTableHeaderRow(headerCells: List<Cell>): List<Cell>? =
        headerCells.mapIndexedNotNull { columnIndex, cell ->
            transformTableHeaderCell(cell, columnIndex)
        }

    fun transformTableHeaderCell(headerCell: Cell, columnIndex: Int): Cell? = headerCell

    fun transformParagraph(originalParagraph: Paragraph): BlockElement? =
        Paragraph(
            originalParagraph.childElements.mapNotNull {
                transformInlineElement(it)
            },
            originalParagraph.metadata
        )

    fun transformListItems(originalListItems: ListItems): BlockElement? =
        ListItems(
            originalListItems.isOrdered,
            originalListItems.items.mapNotNull {
                transformListItem(it)
            },
            originalListItems.metadata
        )

    fun transformListItem(originalListItem: ListItem): ListItem? =
        ListItem(
            originalListItem.contents.mapNotNull {
                transformBlockElement(it)
            },
            originalListItem.metadata
        )

    fun transformLinkDefinition(originalLinkDefinition: LinkDefinition): BlockElement? = originalLinkDefinition

    fun transformHtmlBlock(originalHtmlBlock: HtmlBlock): BlockElement? = originalHtmlBlock

    fun transformHorizontalRule(originalHorizontalRule: HorizontalRule): BlockElement? = originalHorizontalRule

    fun transformHeading(originalHeading: Heading): BlockElement? =
        Heading(
            originalHeading.level,
            originalHeading.heading.mapNotNull {
                transformInlineElement(it)
            },
            originalHeading.contents.mapNotNull {
                transformBlockElement(it)
            },
            originalHeading.metadata
        )

    fun transformCodeBlock(originalCodeBlock: CodeBlock): BlockElement? = originalCodeBlock

    fun transformBlockQuote(originalBlockQuote: BlockQuote): BlockElement? =
        BlockQuote(
            originalBlockQuote.childElements.mapNotNull {
                transformBlockElement(it)
            },
            originalBlockQuote.metadata
        )

    fun transformInlineElement(originalInlineElement: InlineElement): InlineElement? =
        when (originalInlineElement) {
            is HardLineBreak -> transformHardLineBreak(originalInlineElement)
            is HtmlTag -> transformHtmlTag(originalInlineElement)
            is Image -> transformImage(originalInlineElement)
            is InlineCode -> transformInlineCode(originalInlineElement)
            is InlineLink -> transformInlineLink(originalInlineElement)
            is Text -> transformText(originalInlineElement)
            is TextSpan -> transformTextSpan(originalInlineElement)
        }

    fun transformTextSpan(originalTextSpan: TextSpan): InlineElement? =
        TextSpan(
            originalTextSpan.style,
            originalTextSpan.childElements.mapNotNull {
                transformInlineElement(it)
            },
            originalTextSpan.metadata
        )

    fun transformText(originalText: Text): InlineElement? = originalText

    fun transformInlineLink(originalInlineLink: InlineLink): InlineElement? =
        InlineLink(
            originalInlineLink.target,
            originalInlineLink.textElements.mapNotNull {
                transformInlineElement(it)
            },
            originalInlineLink.metadata
        )

    fun transformInlineCode(originalInlineCode: InlineCode): InlineElement? = originalInlineCode

    fun transformImage(originalImage: Image): InlineElement? =
        Image(
            originalImage.url,
            originalImage.descriptionElements.mapNotNull {
                transformInlineElement(it)
            },
            originalImage.metadata
        )

    fun transformHtmlTag(originalHtmlTag: HtmlTag): InlineElement? = originalHtmlTag

    fun transformHardLineBreak(originalHardLineBreak: HardLineBreak): InlineElement? = originalHardLineBreak
}
