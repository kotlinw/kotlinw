package kotlinw.i18n

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class LocaleId
internal constructor(val value: String)

object LocaleIds {

    val en = LocaleId("en")
    val hu = LocaleId("hu")
    val nn = LocaleId("nn")
    val nb = LocaleId("nb")

    val defaultFallbackLocaleId = en

    internal val all = listOf(en, hu, nn, nb).associateBy { it.value }

    fun of(localeIdValue: String, fallbackLocaleId: LocaleId? = defaultFallbackLocaleId): LocaleId =
        all[localeIdValue]
            ?: fallbackLocaleId
            ?: throw IllegalArgumentException("Unsupported locale ID: $localeIdValue")
}

data class Locale(
    val id: LocaleId
)

@Suppress("MemberVisibilityCanBePrivate")
object Locales {

    private val all = LocaleIds.all.values.associateWith { Locale(it) }

    fun of(localeId: LocaleId, fallbackLocaleId: LocaleId? = LocaleIds.defaultFallbackLocaleId): Locale =
        all[localeId]
            ?: fallbackLocaleId?.let { of(it, null) }
            ?: throw IllegalArgumentException("Unsupported locale: $localeId")
}

val LocaleId.language get() = value  // TODO support country and variant
