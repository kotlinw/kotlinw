package xyz.kotlinw.jpa.api

import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

interface ReactiveJpaContext {

    fun <T> watchEntities(watchedEntityTypes: List<KClass<*>>, block: JpaSessionContext.() -> T): Flow<T>
}

fun <T> ReactiveJpaContext.watchEntities(vararg watchedEntityTypes: KClass<*>, block: JpaSessionContext.() -> T): Flow<T> =
    watchEntities(watchedEntityTypes.toList(), block)
