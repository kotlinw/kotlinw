package kotlinw.i18n

import kotlin.jvm.JvmInline
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class LocalizedTextMap<K: LocalizedTextKey>(val value: Map<K, String>)

abstract class ApplicationStrings<K: LocalizedTextKey>(
    private val localizedTexts: LocalizedTextMap<K>,
    private val unknownTranslationPlaceholderProvider: (LocalizedTextKey) -> String = { "[$it]" }
) {

    protected val LocalizedTextKey.translation
        get() = localizedTexts.value[this] ?: unknownTranslationPlaceholderProvider(this)

    protected fun translation(key: LocalizedTextKey) = ReadOnlyProperty { _: Any, _: KProperty<*> -> key.translation }
}
