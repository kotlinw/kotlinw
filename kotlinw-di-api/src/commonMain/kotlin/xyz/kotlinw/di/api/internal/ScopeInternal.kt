package xyz.kotlinw.di.api.internal

import kotlin.reflect.KClass
import xyz.kotlinw.di.api.Scope

class ScopeInternal(
    private val container: ContainerImplementorInternal,
    private val parentScope: ScopeInternal?,
    private val includedModules: Set<KClass<out Any>>
) : Scope {

    init {
        require(includedModules.isNotEmpty())

        if (parentScope != null) {
            require(includedModules.none { parentScope.allIncludedModules.contains(it) })
        }

        require(includedModules.all { container.availableModules.contains(it) })
    }

    private val allIncludedModules get() = (parentScope?.includedModules ?: emptyList()) + includedModules

    override suspend fun close() {
        TODO("Not yet implemented")
    }

    override fun createNestedScope(vararg moduleDeclarations: KClass<out Any>): Scope =
        ScopeInternal(container, this, moduleDeclarations.toSet())

    override fun <T : Any> getSingle(componentClass: KClass<T>): T {
        TODO("Not yet implemented")
    }

    override fun <T : Any> getAll(componentClass: KClass<in T>): List<T> {
        TODO("Not yet implemented")
    }
}
