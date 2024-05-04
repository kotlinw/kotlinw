package xyz.kotlinw.jpa.core

import kotlin.reflect.KClass
import xyz.kotlinw.jpa.api.TypedEntityManager
import xyz.kotlinw.jpa.api.TypedQuery
import xyz.kotlinw.jpa.api.createNamedQuery
import xyz.kotlinw.jpa.api.createQuery
import xyz.kotlinw.jpa.api.setParameters

//
// Type-safe query (query string)
//

fun <R : Any> TypedEntityManager.createTypedQuery(
    qlString: String,
    resultClass: Class<R>,
    vararg arguments: Any?
): TypedQuery<R> =
    createQuery(qlString, resultClass).setParameters(arguments)

fun <R : Any> TypedEntityManager.createTypedQuery(
    qlString: String,
    resultType: KClass<R>,
    vararg arguments: Any?
): TypedQuery<R> =
    createQuery(qlString, resultType).setParameters(arguments)

inline fun <reified R : Any> TypedEntityManager.createTypedQuery(qlString: String, vararg arguments: Any?) =
    createTypedQuery(qlString, R::class, *arguments)

fun <R : Any> TypedEntityManager.executeQuery(
    qlString: String,
    resultType: KClass<R>,
    vararg arguments: Any?
): List<R> =
    createTypedQuery(qlString, resultType, *arguments).resultList

inline fun <reified R : Any> TypedEntityManager.executeQuery(
    qlString: String,
    vararg arguments: Any?
): List<R> =
    executeQuery(qlString, R::class, *arguments)

//
// Type-safe query (named query)
//

fun <R : Any> TypedEntityManager.createTypedNamedQuery(
    name: String,
    resultClass: Class<R>,
    vararg arguments: Any?
): TypedQuery<R> =
    createNamedQuery(name, resultClass).setParameters(arguments)

fun <R : Any> TypedEntityManager.createTypedNamedQuery(
    name: String,
    resultType: KClass<R>,
    vararg arguments: Any?
): TypedQuery<R> =
    createNamedQuery(name, resultType).setParameters(arguments)

inline fun <reified R : Any> TypedEntityManager.createTypedNamedQuery(name: String, vararg arguments: Any?) =
    createTypedNamedQuery(name, R::class, *arguments)

fun <R : Any> TypedEntityManager.executeNamedQuery(
    name: String,
    resultType: KClass<R>,
    vararg arguments: Any?
): List<R> =
    createTypedNamedQuery(name, resultType, *arguments).resultList

inline fun <reified R : Any> TypedEntityManager.executeNamedQuery(
    name: String,
    vararg arguments: Any?
): List<R> =
    executeQuery(name, R::class, *arguments)
