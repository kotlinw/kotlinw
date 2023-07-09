package xyz.kotlinw.markdown.core

import xyz.kotlinw.markdown.core.MarkdownDocumentElement.Image
import xyz.kotlinw.markdown.core.MarkdownDocumentElement.InlineLink

interface MarkdownDocumentInlineUrlResolver {
    fun resolveLinkUrl(linkTarget: String): String

    fun resolveImageUrl(imageUrl: String): String
}

fun MarkdownDocumentModel.resolveInlineUrls(urlResolver: MarkdownDocumentInlineUrlResolver): MarkdownDocumentModel =
    transform(
        object : MarkdownDocumentModelTransformer {
            override fun transformInlineLink(originalInlineLink: InlineLink): InlineLink =
                InlineLink(
                    urlResolver.resolveLinkUrl(originalInlineLink.target),
                    originalInlineLink.textElements,
                    originalInlineLink.metadata
                )

            override fun transformImage(originalImage: Image): Image =
                Image(
                    urlResolver.resolveImageUrl(originalImage.url),
                    originalImage.descriptionElements,
                    originalImage.metadata
                )
        }
    )
