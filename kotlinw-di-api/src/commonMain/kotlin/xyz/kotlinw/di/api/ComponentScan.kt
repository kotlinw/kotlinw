package xyz.kotlinw.di.api

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION

@Retention(BINARY)
@Target(CLASS)
@MustBeDocumented
annotation class ComponentScan
