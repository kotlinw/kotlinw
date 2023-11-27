package xyz.kotlinw.di.api

interface ContainerScope {

    suspend fun close()
}

// TODO
//interface QueryableScope {
//
//    fun <T : Any> getSingleOrNull(componentClass: KClass<T>): T?
//
//    fun <T : Any> getAll(componentClass: KClass<in T>): List<T>
//}
