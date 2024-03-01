package kotlinw.configuration.core

fun interface ConfigurationProvider<out T> {

    companion object {

        suspend fun <T> ConfigurationProvider<T>.getRequiredConfiguration(): T & Any =
            getConfiguration()
                ?: throw ConfigurationException("Failed to lookup configuration: provider=$this")
    }

    suspend fun getConfiguration(): T
}
