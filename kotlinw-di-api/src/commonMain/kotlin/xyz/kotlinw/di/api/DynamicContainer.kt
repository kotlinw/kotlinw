package xyz.kotlinw.di.api

import kotlin.reflect.KClass

interface DynamicContainer {

    fun createRootScope(vararg moduleDeclarations: KClass<out Any>): Scope

    fun createNestedScope(parentScope: Scope, vararg moduleDeclarations: KClass<out Any>): Scope
}
