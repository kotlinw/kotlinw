package xyz.kotlinw.di.api

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.reflect.KClass

@Retention(BINARY)
@Target(CLASS, FUNCTION)
@MustBeDocumented
annotation class Container(vararg val modules: KClass<out Any>)
