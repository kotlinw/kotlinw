package xyz.kotlinw.di.api

import kotlin.reflect.KClass
import kotlin.reflect.typeOf
import xyz.kotlinw.di.api.internal.ModuleImplementorInternal

interface Scope {

    suspend fun close()

    fun createNestedScope(        vararg moduleDeclarations: KClass<out Any>    ): Scope

    fun <T : Any> getSingle(componentClass: KClass<T>): T

    fun <T : Any> getAll(componentClass: KClass<in T>): List<T>
}
