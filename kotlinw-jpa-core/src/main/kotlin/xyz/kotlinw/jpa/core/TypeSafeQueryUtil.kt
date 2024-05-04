package xyz.kotlinw.jpa.core

import kotlin.reflect.KClass
import xyz.kotlinw.jpa.api.TypeSafeEntityManager
import xyz.kotlinw.jpa.api.TypeSafeQuery
import xyz.kotlinw.jpa.api.createNamedQuery
import xyz.kotlinw.jpa.api.createQuery

//
// Type-safe query (query string)
//

fun <R : Any> TypeSafeEntityManager.createTypeSafeQuery(
    qlString: String,
    resultType: KClass<R>,
    vararg arguments: Any?
): TypeSafeQuery<R> =
    createQuery(qlString, resultType).also {
        arguments.forEachIndexed { index, value ->
            it.setParameter(index + 1, value)
        }
    }

inline fun <reified R : Any> TypeSafeEntityManager.createTypeSafeQuery(qlString: String, vararg arguments: Any?) =
    createTypeSafeQuery(qlString, R::class, *arguments)

fun <R : Any> TypeSafeEntityManager.executeQuery(
    qlString: String,
    resultType: KClass<R>,
    vararg arguments: Any?
): List<R> =
    createTypeSafeQuery(qlString, resultType, *arguments).resultList

inline fun <reified R : Any> TypeSafeEntityManager.executeQuery(
    qlString: String,
    vararg arguments: Any?
): List<R> =
    executeQuery(qlString, R::class, *arguments)

//
// Type-safe query (named query)
//

fun <R : Any> TypeSafeEntityManager.createTypeSafeNamedQuery(
    name: String,
    resultType: KClass<R>,
    vararg arguments: Any?
): TypeSafeQuery<R> =
    createNamedQuery(name, resultType).also {
        arguments.forEachIndexed { index, value ->
            it.setParameter(index + 1, value)
        }
    }

inline fun <reified R : Any> TypeSafeEntityManager.createTypeSafeNamedQuery(name: String, vararg arguments: Any?) =
    createTypeSafeNamedQuery(name, R::class, *arguments)

fun <R : Any> TypeSafeEntityManager.executeNamedQuery(
    name: String,
    resultType: KClass<R>,
    vararg arguments: Any?
): List<R> =
    createTypeSafeNamedQuery(name, resultType, *arguments).resultList

inline fun <reified R : Any> TypeSafeEntityManager.executeNamedQuery(
    name: String,
    vararg arguments: Any?
): List<R> =
    executeQuery(name, R::class, *arguments)
