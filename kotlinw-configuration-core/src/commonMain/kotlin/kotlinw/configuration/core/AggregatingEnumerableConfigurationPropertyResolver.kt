package kotlinw.configuration.core

class AggregatingEnumerableConfigurationPropertyResolver(
    delegatesByDecreasingPriority: List<EnumerableConfigurationPropertyResolver>
) : EnumerableConfigurationPropertyResolver {

    private val delegatesByIncreasingPriority = delegatesByDecreasingPriority.asReversed()

    override suspend fun initialize() {
        delegatesByIncreasingPriority.forEach {
            it.initialize()
        }
    }

    override fun getPropertyKeys(): Set<ConfigurationPropertyKey> =
        delegatesByIncreasingPriority.flatMap { it.getPropertyKeys() }.toSet()

    override fun getPropertyValueOrNull(key: ConfigurationPropertyKey): EncodedConfigurationPropertyValue? =
        delegatesByIncreasingPriority.firstNotNullOfOrNull { it.getPropertyValueOrNull(key) }
}
