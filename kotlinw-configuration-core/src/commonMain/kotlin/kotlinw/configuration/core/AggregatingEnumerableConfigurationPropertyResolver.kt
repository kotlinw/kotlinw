package kotlinw.configuration.core

class AggregatingEnumerableConfigurationPropertyResolver(
    delegatesByDecreasingPriority: List<EnumerableConfigurationPropertyResolver>
) : EnumerableConfigurationPropertyResolver {

    private val delegatesByIncreasingPriority = delegatesByDecreasingPriority.asReversed()

    override suspend fun reload() {
        delegatesByIncreasingPriority.forEach {
            it.reload()
        }
    }

    override fun getPropertyKeys(): Set<ConfigurationPropertyKey> =
        delegatesByIncreasingPriority.flatMap { it.getPropertyKeys() }.toSet()

    override fun getPropertyValueOrNull(key: ConfigurationPropertyKey): EncodedConfigurationPropertyValue? =
        delegatesByIncreasingPriority.firstNotNullOfOrNull { it.getPropertyValueOrNull(key) }

    override fun toString(): String {
        return "AggregatingEnumerableConfigurationPropertyResolver(delegatesByIncreasingPriority=$delegatesByIncreasingPriority)"
    }
}
