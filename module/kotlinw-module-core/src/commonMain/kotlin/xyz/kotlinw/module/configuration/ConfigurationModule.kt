package xyz.kotlinw.module.configuration

import kotlinw.configuration.core.ConfigurationObjectLookup
import kotlinw.configuration.core.ConfigurationObjectLookupImpl
import kotlinw.configuration.core.ConfigurationPropertyLookup
import kotlinw.configuration.core.ConfigurationPropertyLookupImpl
import kotlinw.configuration.core.ConfigurationPropertyLookupSource
import kotlinw.logging.api.LoggerFactory
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.Module

@Module
class ConfigurationModule {

    @Component
    suspend fun configurationPropertyLookup(
        loggerFactory: LoggerFactory,
        configurationPropertyLookupSources: List<ConfigurationPropertyLookupSource>
    ): ConfigurationPropertyLookup =
        ConfigurationPropertyLookupImpl(loggerFactory, configurationPropertyLookupSources).also { it.initialize() }

    @Component
    fun configurationObjectLookup(configurationPropertyLookup: ConfigurationPropertyLookup): ConfigurationObjectLookup =
        ConfigurationObjectLookupImpl(configurationPropertyLookup)
}
