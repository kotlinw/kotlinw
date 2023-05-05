package kotlinw.configuration.core

import kotlinw.util.stdlib.HasPriority
import kotlin.reflect.KClass

interface ConfigurationPropertyValueConverter {

    fun <T : Any> decode(value: String, targetType: KClass<T>): T?
}

class ConfigurationPropertyValueConverterImpl(
    parsers: Iterable<ConfigurationPropertyValueParser<*>>
) : ConfigurationPropertyValueConverter {

    companion object {

        val defaultParsers = listOf(
            StringConfigurationPropertyValueParser,
            IntConfigurationPropertyValueParser,
            BooleanConfigurationPropertyValueParser,
            JsonObjectConfigurationPropertyValueParser
        )
    }

    private val configurationValueParsers = parsers.sortedWith(HasPriority.comparator)

    override fun <T : Any> decode(value: String, targetType: KClass<T>): T? {
        if (value.isEmpty()) {
            return null
        }

        if (value.isBlank()) {
            throw ConfigurationException("Blank configuration property value is not allowed.")
        }

        val parser = configurationValueParsers.firstOrNull { it.supports(targetType) }
            ?: throw ConfigurationException("No parser found: value=$value, targetType=$targetType")

        val parsedValue = try {
            parser.parseConfigurationPropertyValue(value, targetType)
        } catch (e: Exception) {
            throw ConfigurationException("Parsing failed: value=$value, targetType=$targetType", e)
        }

        if (!targetType.isInstance(parsedValue)) {
            throw ConfigurationException("Unexpected parsed value: value=$value, targetType=$targetType, parsedValue=$parsedValue")
        }

        @Suppress("UNCHECKED_CAST")
        return parsedValue as T
    }
}
