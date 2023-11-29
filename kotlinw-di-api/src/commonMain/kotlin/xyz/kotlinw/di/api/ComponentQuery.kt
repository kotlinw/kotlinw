package xyz.kotlinw.di.api

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY

@Retention(BINARY)
@Target(FUNCTION, PROPERTY)
@MustBeDocumented
annotation class ComponentQuery
