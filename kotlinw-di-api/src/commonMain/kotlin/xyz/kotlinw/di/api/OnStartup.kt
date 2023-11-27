package xyz.kotlinw.di.api

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.FUNCTION

@Retention(BINARY)
@Target(FUNCTION)
@MustBeDocumented
annotation class OnStartup
