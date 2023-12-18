package xyz.kotlinw.module.configuration

import kotlinw.configuration.core.ConfigurationObjectLookup
import kotlinw.configuration.core.ConfigurationObjectLookupImpl
import kotlinw.configuration.core.ConfigurationPropertyLookup
import kotlinw.configuration.core.ConfigurationPropertyLookupImpl
import kotlinw.configuration.core.ConfigurationPropertyLookupSource
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.Module

@Module
class ConfigurationModule {

    @Component
    suspend fun configurationPropertyLookup(configurationPropertyLookupSources: List<ConfigurationPropertyLookupSource>): ConfigurationPropertyLookup =
        ConfigurationPropertyLookupImpl(configurationPropertyLookupSources).also { it.initialize() }

    @Component
    fun configurationObjectLookup(configurationPropertyLookup: ConfigurationPropertyLookup): ConfigurationObjectLookup =
        ConfigurationObjectLookupImpl(configurationPropertyLookup)
}
