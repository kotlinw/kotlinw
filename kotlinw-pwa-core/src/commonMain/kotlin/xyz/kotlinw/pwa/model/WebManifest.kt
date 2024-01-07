package xyz.kotlinw.pwa.model

import kotlin.jvm.JvmOverloads
import kotlinw.util.stdlib.Url
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import xyz.kotlinw.pwa.model.WebManifest.Display.Browser
import xyz.kotlinw.pwa.model.WebManifest.ImageResourceSize.Companion.decodeFromString
import xyz.kotlinw.pwa.model.WebManifest.ImageResourceSize.Companion.encodeToString
import xyz.kotlinw.pwa.model.WebManifest.Method.GET
import xyz.kotlinw.pwa.model.WebManifest.ShortcutItem

/**
 * See:
 * - [MDN/Web/Manifest](https://developer.mozilla.org/en-US/docs/Web/Manifest)
 * - [Web Application Manifest specification](https://w3c.github.io/manifest/)
 * - [manifest.webmanifest JSON schema](https://json.schemastore.org/web-manifest.json)
 *
 * [Localizable members](https://w3c.github.io/manifest/#dfn-localizable-members):
 * - [description]
 * - [name]
 * - [shortName]
 * - [ShortcutItem.name]
 * - [ShortcutItem.shortName]
 * - [ShortcutItem.description]
 */
@Serializable
data class WebManifest(
    /**
     * The name of the web application.
     */
    val name: String? = null,

    /**
     * A string that represents a short version of the name of the web application.
     */
    @SerialName("short_name")
    val shortName: String? = null,

    /**
     * The icons member is an array of icon objects that can serve as iconic representations of the web application in various contexts.
     */
    val icons: List<ImageResource>? = null,

    /**
     * Represents the URL that the developer would prefer the user agent load when the user launches the web application.
     */
    @SerialName("start_url")
    val startUrl: String? = null,

    /**
     * The item represents the developer's preferred display mode for the web application.
     */
    val display: Display = Browser,

    /**
     * A string that represents the id of the web application.
     */
    val id: String? = null,

    /**
     * The theme_color member serves as the default theme color for an application context.
     */
    @SerialName("theme_color")
    val themeColor: String? = null,

    /**
     * The background_color member describes the expected background color of the web application.
     */
    @SerialName("background_color")
    val backgroundColor: String? = null,

    /**
     * A string that represents the navigation scope of this web application's application context.
     */
    val scope: String? = null,

    /**
     * The primary language for the values of the manifest.
     */
    @SerialName("lang")
    val language: String? = null,

    /**
     * The base direction for the localizable members of the manifest.
     * */
    @SerialName("dir")
    val direction: Direction? = null,

    /**
     * The orientation member is a string that serves as the default orientation for all  top-level browsing contexts of the web application.
     */
    val orientation: Orientation? = null,

    /**
     *  Description of the purpose of the web application
     */
    val description: String? = null,

    /**
     * The screenshots member is an array of image objects represent the web application in common usage scenarios.
     */
    val screenshots: List<ImageResource>? = null,

    /**
     * Describes the expected application categories to which the web application belongs.
     */
    val categories: List<String>? = null,

    /**
     * Represents an ID value of the IARC rating of the web application. It is intended to be used to determine which ages the web application is appropriate for.
     */
    @SerialName("iarc_rating_id")
    val iarcRatingId: String? = null,

    /**
     * Array of shortcut items that provide access to key tasks within a web application.
     */
    val shortcuts: List<ShortcutItem>? = null,

    /**
     * Declares the application to be a web share target, and describes how it receives share data.
     */
    @SerialName("share_target")
    val shareTarget: ShareTarget? = null,

    /**
     * Boolean value that is used as a hint for the user agent to say that related applications should be preferred over the web application.
     */
    @SerialName("prefer_related_applications")
    val preferRelatedApplications: Boolean? = null,

    /**
     * Array of application accessible to the underlying application platform that has a relationship with the web application.
     */
    @SerialName("related_applications")
    val relatedApplications: List<ExternalApplicationResource>? = null
) {

    @Serializable
    enum class Orientation {

        @SerialName("any")
        Any,

        @SerialName("natural")
        Natural,

        @SerialName("landscape")
        Landscape,

        @SerialName("portrait")
        Portrait,

        @SerialName("portrait-primary")
        PortraitPrimary,

        @SerialName("portrait-secondary")
        PortraitSecondary,

        @SerialName("landscape-primary")
        LandscapePrimary,

        @SerialName("landscape-secondary")
        LandscapeSecondary
    }

    @Serializable
    enum class Display {

        @SerialName("fullscreen")
        FullScreen,

        @SerialName("standalone")
        Standalone,

        @SerialName("minimal-ui")
        MinimalUi,

        @SerialName("browser")
        Browser
    }

    /**
     * The base direction for the localizable members of the manifest.
     */
    @Serializable
    enum class Direction {

        @SerialName("ltr")
        LTR,

        @SerialName("rtl")
        RTL,

        @SerialName("auto")
        Auto
    }

    @Serializable(with = WebManifest.ImageResourceSizes.Companion::class)
    data class ImageResourceSizes(val sizes: List<ImageResourceSize>) {

        companion object : KSerializer<ImageResourceSizes> {

            override val descriptor = PrimitiveSerialDescriptor("ImageResourceSizes", PrimitiveKind.STRING)

            private const val delimiter = " "

            override fun deserialize(decoder: Decoder): ImageResourceSizes {
                val value = decoder.decodeString()
                return ImageResourceSizes(value.split(delimiter).map { decodeFromString(it) })
            }

            override fun serialize(encoder: Encoder, value: ImageResourceSizes) {
                encoder.encodeString(value.sizes.joinToString(delimiter) { encodeToString(it) })
            }
        }

        constructor(vararg sizes: ImageResourceSize) : this(sizes.toList())
    }

    @Serializable(with = ImageResourceSize.Companion::class)
    data class ImageResourceSize(val width: Int, val height: Int) {

        companion object : KSerializer<ImageResourceSize> {

            override val descriptor = PrimitiveSerialDescriptor("ImageResourceSize", PrimitiveKind.STRING)

            private val regex = Regex("(%d)x(%d)")

            internal fun decodeFromString(value: String): ImageResourceSize {
                val matcher = regex.matchEntire(value)
                return matcher?.let {
                    val widthValue = it.groupValues[1]
                    val heightValue = it.groupValues[2]
                    ImageResourceSize(
                        try {
                            widthValue.toInt()
                        } catch (e: Exception) {
                            throw RuntimeException("Invalid width: '$widthValue'")
                        },
                        try {
                            heightValue.toInt()
                        } catch (e: Exception) {
                            throw RuntimeException("Invalid height: '$heightValue'")
                        }
                    )
                } ?: throw RuntimeException("Cannot deserialize: '$value'")
            }

            override fun deserialize(decoder: Decoder): ImageResourceSize = decodeFromString(decoder.decodeString())

            internal fun encodeToString(value: ImageResourceSize) = "${value.width}x${value.height}"

            override fun serialize(encoder: Encoder, value: ImageResourceSize) {
                encoder.encodeString(encodeToString(value))
            }
        }
    }

    @Serializable
    data class ImageResource(
        /**
         * The src member of an image is a URL from which a user agent can fetch the icon's data.
         */
        val src: String,

        /**
         * The type member of an image is a hint as to the media type of the image.
         */
        val type: String? = null,

        /**
         * The sizes member is a string consisting of an unordered set of unique space-separated tokens which are ASCII case-insensitive that represents the dimensions of an image for visual media.
         */
        val sizes: ImageResourceSizes? = null,

        val purpose: String = "any"
    ) {
        init {
            if (type != null) {
                require(cg_regex1.containsMatchIn(type)) { "type does not match pattern $cg_regex1 - $type" }
            }
            require(purpose in cg_array2) { "purpose not in enumerated values - $purpose" }
        }

        @JvmOverloads
        constructor(src: String, type: String, size: ImageResourceSize, purpose: String = "any") :
                this(src, type, ImageResourceSizes(size), purpose)
    }

    @Serializable
    data class ExternalApplicationResource(
        /**
         * The platform it is associated to.
         */
        val platform: Platform,

        /**
         * The URL where the application can be found.
         */
        val url: Url? = null,

        /**
         * Information additional to the URL or instead of the URL, depending on the platform.
         */
        val id: String? = null,

        /**
         * Information about the minimum version of an application related to this web app.
         */
        @SerialName("min_version")
        val minVersion: String? = null,

        /**
         * An array of fingerprint objects used for verifying the application.
         */
        val fingerprints: List<Fingerprint>? = null
    )

    /**
     * The platform it is associated to.
     */
    @Serializable
    enum class Platform {
        @SerialName("chrome_web_store")
        ChromeWebStore,

        @SerialName("play")
        Play,

        @SerialName("itunes")
        iTunes,

        @SerialName("windows")
        Windows
    }

    @Serializable
    data class Fingerprint(
        val type: String? = null,
        val value: String? = null
    )

    @Serializable
    data class ShortcutItem(
        /**
         * The name member of a shortcut item is a string that represents the name of the shortcut as it is usually displayed to the user in a context menu.
         */
        val name: String,

        /**
         * The short_name member of a shortcut item is a string that represents a short version of the name of the shortcut. It is intended to be used where there is insufficient space to display the full name of the shortcut.
         */
        @SerialName("short_name")
        val shortName: String? = null,

        /**
         * The description member of a shortcut item is a string that allows the developer to describe the purpose of the shortcut.
         */
        val description: String? = null,

        /**
         * The url member of a shortcut item is a URL within scope of a processed manifest that opens when the associated shortcut is activated.
         */
        val url: String,

        /**
         * The icons member of a shortcut item serves as iconic representations of the shortcut in various contexts.
         */
        val icons: List<ImageResource>? = null
    )

    /**
     * Declares the application to be a web share target, and describes how it receives share data.
     */
    @Serializable
    data class ShareTarget(
        /**
         * The URL for the web share target.
         */
        val action: String,

        /**
         * The HTTP request method for the web share target.
         */
        val method: Method = GET,

        /**
         * This member specifies the encoding in the share request.
         */
        val enctype: String = "application/x-www-form-urlencoded",

        /**
         * Specifies what data gets shared in the request.
         */
        val params: ShareTargetParameters
    ) {

        init {
            require(enctype in cg_array4) { "enctype not in enumerated values - $enctype" }
        }

    }

    /**
     * The HTTP request method for the web share target.
     */
    enum class Method {
        GET,
        POST
    }

    /**
     * Specifies what data gets shared in the request.
     */
    @Serializable
    data class ShareTargetParameters(
        /**
         * The name of the query parameter used for the title of the document being shared.
         */
        val title: String? = null,

        /**
         * The name of the query parameter used for the message body, made of arbitrary text.
         */
        val text: String? = null,

        /**
         * The name of the query parameter used for the URL string referring to a resource being shared.
         */
        val url: String? = null
    )

    companion object {

        private val cg_regex1 = Regex("^[\\sa-z0-9\\-+;.=/]+\$")

        private val cg_array2 = setOf(
            "monochrome",
            "maskable",
            "any",
            "monochrome maskable",
            "monochrome any",
            "maskable monochrome",
            "maskable any",
            "any monochrome",
            "any maskable",
            "monochrome maskable any",
            "monochrome any maskable",
            "maskable monochrome any",
            "maskable any monochrome",
            "any monochrome maskable",
            "any maskable monochrome"
        )

        private val cg_array4 = setOf(
            "application/x-www-form-urlencoded",
            "multipart/form-data",
            "APPLICATION/X-WWW-FORM-URLENCODED",
            "MULTIPART/FORM-DATA"
        )
    }
}
