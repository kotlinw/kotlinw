package xyz.kotlinw.jpa.api

import kotlin.reflect.KClass

fun <T : Any> TypeSafeEntityManager.createQuery(qlString: String, resultClass: KClass<T>): TypeSafeQuery<T> =
    createQuery(qlString, resultClass.java)

context(TypeSafeEntityManager)
inline fun <reified T : Any> createQuery(qlString: String): TypeSafeQuery<T> =
    createQuery(qlString, T::class)

fun <T : Any> TypeSafeEntityManager.createNamedQuery(name: String, resultClass: KClass<T>): TypeSafeQuery<T> =
    createNamedQuery(name, resultClass.java)

context(TypeSafeEntityManager)
inline fun <reified T : Any> createNamedQuery(name: String): TypeSafeQuery<T> =
    createNamedQuery(name, T::class)
