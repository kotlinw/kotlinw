package xyz.kotlinw.pwa.core

import kotlinw.i18n.LocaleId
import kotlinw.i18n.language
import kotlinw.util.stdlib.Url
import xyz.kotlinw.pwa.model.WebManifest
import xyz.kotlinw.pwa.model.WebManifest.Direction
import xyz.kotlinw.pwa.model.WebManifest.Display
import xyz.kotlinw.pwa.model.WebManifest.Display.Standalone
import xyz.kotlinw.pwa.model.WebManifest.ExternalApplicationResource
import xyz.kotlinw.pwa.model.WebManifest.ImageResource
import xyz.kotlinw.pwa.model.WebManifest.Orientation
import xyz.kotlinw.pwa.model.WebManifest.ShareTarget
import xyz.kotlinw.pwa.model.WebManifest.ShortcutItem

interface WebManifestFactory {

    fun createWebManifest(localeId: LocaleId): WebManifest
}

class WebManifestFactoryImpl(
    private val webManifestAttributeProvider: WebManifestAttributeProvider
): WebManifestFactory {

    override fun createWebManifest(localeId: LocaleId): WebManifest =
        with(webManifestAttributeProvider) {
            WebManifest(
                name(localeId),
                shortName(localeId),
                icons(),
                startUrl().value,
                display(),
                id(),
                themeColor(),
                backgroundColor(),
                scope(),
                localeId.language,
                direction(),
                orientation(),
                description(localeId),
                screenshots(),
                categories(),
                iarcRatingId(),
                shortcuts(localeId),
                shareTarget(),
                preferRelatedApplications(),
                relatedApplications()
            )
        }
}

interface WebManifestAttributeProvider {

    val fallbackLocaleId: LocaleId

    fun name(localeId: LocaleId): String

    fun shortName(localeId: LocaleId): String

    fun icons(): List<ImageResource>

    fun startUrl(): Url

    fun display(): Display = Standalone

    fun id(): String? = null

    fun themeColor(): String? = null

    fun backgroundColor(): String? = null

    fun scope(): String? = null

    fun direction(): Direction? = null

    fun orientation(): Orientation? = null

    fun description(localeId: LocaleId): String? = null

    fun screenshots(): List<ImageResource>? = null

    fun categories(): List<String>? = null

    fun iarcRatingId(): String? = null

    fun shortcuts(localeId: LocaleId): List<ShortcutItem>? = null

    fun shareTarget(): ShareTarget? = null

    fun preferRelatedApplications(): Boolean? = null

    fun relatedApplications(): List<ExternalApplicationResource>? = null
}
