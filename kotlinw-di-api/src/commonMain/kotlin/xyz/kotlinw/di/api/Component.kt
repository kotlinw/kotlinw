package xyz.kotlinw.di.api

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER
import kotlin.reflect.KClass

@Retention(BINARY)
@Target(CLASS, FUNCTION, VALUE_PARAMETER)
@MustBeDocumented
annotation class Component(
    val type: KClass<out Any> = Any::class, // TODO ez tömb legyen, hogy több interfészt is lehessen megadni
    val onConstruction: String = "",
    val onTerminate: String = ""
)
