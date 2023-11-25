package xyz.kotlinw.di.api.internal

import kotlin.reflect.KClass
import xyz.kotlinw.di.api.DynamicContainer
import xyz.kotlinw.di.api.Scope

abstract class ContainerImplementorInternal(
    internal val containerMetadata: ContainerMetadata?
): DynamicContainer {

    override fun createRootScope(vararg moduleDeclarations: KClass<out Any>): Scope =
        createScope(null, moduleDeclarations.toSet())

    override fun createNestedScope(parentScope: Scope, vararg moduleDeclarations: KClass<out Any>): Scope =
        createScope(parentScope, moduleDeclarations.toSet())

    private fun createScope(
        parentScope: Scope?,
        moduleDeclarations: Set<KClass<out Any>>
    ): Scope {
        require(parentScope is ScopeInternal)
        require(moduleDeclarations.isNotEmpty())
        // TODO
//        moduleDeclarations
//            .filter { !availableModules.contains(it) }
//            .also {
//                require(it.isEmpty()) {
//                    "Some modules are not available for the container: ${it.joinToString()}. Available modules: ${availableModules.joinToString()}"
//                }
//            }

        TODO()
    }
}
