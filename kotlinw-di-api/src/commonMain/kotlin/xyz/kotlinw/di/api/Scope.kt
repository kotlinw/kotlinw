package xyz.kotlinw.di.api

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.reflect.KClass

@Retention(BINARY)
@Target(FUNCTION)
@MustBeDocumented
annotation class Scope(val modules: Array<KClass<out Any>>, val parent: String = "", val ignoredComponents: Array<IgnoredComponent> = [])  {

    @Retention(BINARY)
    @MustBeDocumented
    annotation class IgnoredComponent(val module: KClass<out Any>, val localComponentId: String)
}
