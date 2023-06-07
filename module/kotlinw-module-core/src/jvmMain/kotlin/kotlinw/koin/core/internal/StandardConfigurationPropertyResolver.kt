package kotlinw.koin.core.internal

import kotlinw.configuration.core.ConfigurationPropertyKey
import kotlinw.configuration.core.DeploymentMode
import kotlinw.configuration.core.EnumerableConfigurationPropertyResolver
import kotlinw.configuration.core.StandardJvmConfigurationPropertyResolver

class StandardConfigurationPropertyResolver(deploymentMode: DeploymentMode, classLoader: ClassLoader) :
    EnumerableConfigurationPropertyResolver {

    private val delegate: EnumerableConfigurationPropertyResolver =
        StandardJvmConfigurationPropertyResolver(deploymentMode, classLoader)

    override suspend fun initialize() {
        delegate.initialize()
    }

    override fun getPropertyKeys() = delegate.getPropertyKeys()

    override fun getPropertyValueOrNull(key: ConfigurationPropertyKey) = delegate.getPropertyValueOrNull(key)
}
