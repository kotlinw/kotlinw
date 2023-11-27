package xyz.kotlinw.di.api.internal

import xyz.kotlinw.di.api.ContainerScope

abstract class ScopeInternal(private val parentScope: ScopeInternal?) : ContainerScope {

    override suspend fun close() {
        TODO("Not yet implemented")
    }
}

sealed interface ScopeBuilder {

    fun registerComponent(componentId: ComponentId, instance: Any)

    fun getComponent(componentId: ComponentId): Any
}

private class ScopeBuilderImpl : ScopeBuilder {

    val components = mutableMapOf<ComponentId, Any>()

    override fun registerComponent(componentId: ComponentId, instance: Any) {
        check(!components.containsKey(componentId))
        components[componentId] = instance
    }

    override fun getComponent(componentId: ComponentId) =
        components[componentId] ?: throw IllegalStateException("Unknown component: $componentId")
}
