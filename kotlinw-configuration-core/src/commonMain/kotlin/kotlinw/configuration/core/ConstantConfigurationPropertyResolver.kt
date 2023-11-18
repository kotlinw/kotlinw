package kotlinw.configuration.core

class ConstantConfigurationPropertyResolver(
    private val properties: Map<ConfigurationPropertyKey, EncodedConfigurationPropertyValue>
) : EnumerableConfigurationPropertyResolver {

    companion object {

        fun of(properties: Map<String, EncodedConfigurationPropertyValue>) =
            ConstantConfigurationPropertyResolver(properties.mapKeys { ConfigurationPropertyKey(it.key) })

        fun of(vararg properties: Pair<String, EncodedConfigurationPropertyValue>) =
            of(properties.toList().toMap())
    }

    override suspend fun initialize() {}

    override fun getPropertyValueOrNull(key: ConfigurationPropertyKey): EncodedConfigurationPropertyValue? =
        properties[key]

    override fun getPropertyKeys(): Set<ConfigurationPropertyKey> = properties.keys
}
