package kotlinw.configuration.core

import kotlinw.util.stdlib.HasPriority
import kotlinw.util.stdlib.Priority
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.reflect.KClass

interface ConfigurationPropertyValueParser<T> : HasPriority {

    fun supports(targetType: KClass<*>): Boolean

    fun parseConfigurationPropertyValue(value: String, targetType: KClass<*>): T
}

abstract class AbstractSimpleConfigurationPropertyValueParser<T>(
    private val supportedTargetType: KClass<*>,
    override val priority: Priority = Priority.Normal
) : ConfigurationPropertyValueParser<T> {

    final override fun supports(targetType: KClass<*>): Boolean = targetType == supportedTargetType

    final override fun parseConfigurationPropertyValue(value: String, targetType: KClass<*>): T =
        parseConfigurationPropertyValue(value)

    protected abstract fun parseConfigurationPropertyValue(value: String): T
}

object StringConfigurationPropertyValueParser : AbstractSimpleConfigurationPropertyValueParser<String>(String::class) {

    override fun parseConfigurationPropertyValue(value: String): String = value
}

object IntConfigurationPropertyValueParser : AbstractSimpleConfigurationPropertyValueParser<Int>(Int::class) {

    override fun parseConfigurationPropertyValue(value: String): Int = value.toInt()
}

object BooleanConfigurationPropertyValueParser :
    AbstractSimpleConfigurationPropertyValueParser<Boolean>(Boolean::class) {

    override fun parseConfigurationPropertyValue(value: String): Boolean = value.toBooleanStrict()
}

object JsonObjectConfigurationPropertyValueParser :
    AbstractSimpleConfigurationPropertyValueParser<JsonObject>(JsonObject::class) {

    private val json = Json

    override fun parseConfigurationPropertyValue(value: String): JsonObject = json.decodeFromString<JsonObject>(value)
}
