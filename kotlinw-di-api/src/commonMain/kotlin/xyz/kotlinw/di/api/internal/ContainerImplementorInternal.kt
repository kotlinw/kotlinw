package xyz.kotlinw.di.api.internal

import kotlin.reflect.KClass
import xyz.kotlinw.di.api.ContainerImplementor
import xyz.kotlinw.di.api.Scope

class ContainerImplementorInternal(
    val availableModules: Map<KClass<out Any>, ModuleImplementorInternal>
) : ContainerImplementor {

    init {
        require(availableModules.isNotEmpty())
    }

    override fun createRootScope(vararg moduleDeclarations: KClass<out Any>): Scope =
        createScope(null, moduleDeclarations.toSet())

    private fun createScope(parentScope: Scope?, moduleDeclarations: Set<KClass<out Any>>): Scope {
        require(parentScope is ScopeInternal)
        require(moduleDeclarations.isNotEmpty())
        moduleDeclarations
            .filter { !availableModules.containsKey(it) }
            .also {
                require(it.isEmpty()) {
                    "Some modules are not available for the container: ${it.joinToString()}. Available modules: ${availableModules.keys.joinToString()}"
                }
            }

        return ScopeInternal(this, parentScope, moduleDeclarations)
    }
}
