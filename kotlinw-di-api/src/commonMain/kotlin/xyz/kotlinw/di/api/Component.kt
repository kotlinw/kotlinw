package xyz.kotlinw.di.api

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER
import kotlin.reflect.KClass

/**
 * Annotation used to mark a source code element as a KotlinW component.
 *
 * @param onConstruction The code to be executed during the construction of the component instance.
 * @param onTerminate The code to be executed when the container instance containing the component instance is terminated.
 */
@Retention(BINARY)
@Target(CLASS, FUNCTION, VALUE_PARAMETER)
@MustBeDocumented
annotation class Component(
    val type: KClass<out Any> = Any::class, // TODO itt még egy olyan fícsör is lehetne, hogy azt adja meg, hogy melyik típust rejti el
    // TODO val types: Array<KClass<out Any>> = [],
    val onConstruction: String = "",
    val onTerminate: String = ""
)
