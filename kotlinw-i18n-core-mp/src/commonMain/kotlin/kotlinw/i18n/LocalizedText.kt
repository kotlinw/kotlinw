package kotlinw.i18n

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

interface ILocalizedText {
    operator fun get(localeId: LocaleId): String?
}

@Serializable
@JvmInline
value class LocalizedText(val translations: Map<LocaleId, String>) : ILocalizedText {

    constructor(translation: Pair<LocaleId, String>, vararg translations: Pair<LocaleId, String?>) :
            this(
                (translations.toList() + translation)
                    .filter { it.second != null }
                    .associate { it as Pair<LocaleId, String> }
            )

    init {
        require(translations.isNotEmpty())
    }

    override operator fun get(localeId: LocaleId): String? = translations[localeId]
}

fun LocalizedText.mutate(block: (MutableLocalizedText) -> Unit): LocalizedText {
    val mutable = MutableLocalizedText(this)
    block(mutable)
    return LocalizedText(mutable.translations)
}

class MutableLocalizedText(source: LocalizedText) {
    val translations: MutableMap<LocaleId, String> = source.translations.toMutableMap()

    operator fun set(localeId: LocaleId, translation: String?) {
        if (translation != null) {
            translations[localeId] = translation
        } else {
            translations.remove(localeId)
        }
    }
}

const val NoTranslationAvailable = "?"

operator fun LocalizedText?.get(localeId: LocaleId): String? = this?.get(localeId)

// TODO
// context(LocaleContext)
// val LocalizedText?.contextTranslation get() = getSafeTranslation()

fun LocalizedText?.getSafeTranslation(localeId: LocaleId): String =
    get(localeId) ?: NoTranslationAvailable
