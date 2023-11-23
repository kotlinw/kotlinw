package xyz.kotlinw.di.api

import kotlin.reflect.KClass

interface ContainerImplementor {

    fun createRootScope(vararg moduleDeclarations: KClass<out Any>): Scope
}
