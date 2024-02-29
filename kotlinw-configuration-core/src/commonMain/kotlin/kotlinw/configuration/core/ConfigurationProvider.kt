package kotlinw.configuration.core

fun interface ConfigurationProvider<T> {

    suspend fun getConfiguration(): T
}
