package xyz.kotlinw.di.api

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.FUNCTION

@Retention(BINARY)
@Target(FUNCTION)
@MustBeDocumented
annotation class OnConstruction

// TODO egy ötlet: ha az ezzel annotált metódusnak context(CoroutineScope)-ja van, akkor az ApplicationCoroutineService szkópját adjuk neki át
