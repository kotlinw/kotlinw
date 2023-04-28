package kotlinw.configuration.core

import kotlinw.util.stdlib.HasPriority
import kotlin.reflect.KClass

interface ConfigurationPropertyValueResolver {

    fun <T : Any> parseConfigurationPropertyValue(value: String, targetType: KClass<T>): T
}

class ConfigurationPropertyValueResolverImpl(
    configurationPropertyValueParsers: Iterable<ConfigurationPropertyValueParser<*>>
) : ConfigurationPropertyValueResolver {

    companion object {

        val defaultParsers = listOf(
            StringConfigurationPropertyValueParser,
            IntConfigurationPropertyValueParser,
            BooleanConfigurationPropertyValueParser,
            JsonObjectConfigurationPropertyValueParser
        )
    }

    private val configurationValueParsers = configurationPropertyValueParsers.sortedWith(HasPriority.comparator)

    override fun <T : Any> parseConfigurationPropertyValue(value: String, targetType: KClass<T>): T {
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
