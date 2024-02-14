package xyz.kotlinw.di.api

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.ANNOTATION_CLASS
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.TYPE
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER

/**
 * A qualifier is used to distinguish between components of the same type.
 */
@Retention(BINARY)
@Target(FUNCTION, CLASS, TYPE, ANNOTATION_CLASS)
@MustBeDocumented
annotation class Qualifier(val value: String)

// TODO implement qualifier annotations:
// @Retention(BINARY)
// @Target(FUNCTION, CLASS, TYPE)
// @MustBeDocumented
// @Qualifier("my-qualifier")
// annotation class MyQualifier
