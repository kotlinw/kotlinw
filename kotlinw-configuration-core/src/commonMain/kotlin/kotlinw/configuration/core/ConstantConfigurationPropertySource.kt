package kotlinw.configuration.core

class ConstantConfigurationPropertySource(
    private val properties: Map<String, String>
) : AbstractConfigurationPropertySource(), EnumerableConfigurationPropertySource {

    override fun getPropertyValue(key: String): String? = properties[key]

    override fun getPropertyKeys(): Set<String> = properties.keys
}
