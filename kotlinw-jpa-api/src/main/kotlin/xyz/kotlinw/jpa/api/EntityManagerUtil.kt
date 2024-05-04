package xyz.kotlinw.jpa.api

import jakarta.persistence.EntityGraph
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityNotFoundException
import jakarta.persistence.LockModeType
import jakarta.persistence.TypedQuery
import kotlin.reflect.KClass

fun <E : Any> EntityManager.getReferenceOrNull(entityClass: Class<E>, primaryKey: Any): E? =
    try {
        getReference(entityClass, primaryKey)
    } catch (e: EntityNotFoundException) {
        null
    }

//
// `findOrNull()` alternatives for `find()` methods
//

fun <T : Any> EntityManager.findOrNull(entityClass: Class<T>, primaryKey: Any): T? =
    find(entityClass, primaryKey)

fun <T : Any> EntityManager.findOrNull(entityClass: Class<T>, primaryKey: Any, properties: Map<String, Any>): T? =
    find(entityClass, primaryKey, properties)

fun <T : Any> EntityManager.findOrNull(entityClass: Class<T>, primaryKey: Any, lockMode: LockModeType): T? =
    find(entityClass, primaryKey, lockMode)

fun <T : Any> EntityManager.findOrNull(
    entityClass: Class<T>,
    primaryKey: Any,
    lockMode: LockModeType,
    properties: Map<String, Any>
): T? =
    find(entityClass, primaryKey, lockMode, properties)

//
// KClass<T> alternatives for Class<T>
//

fun <T : Any> EntityManager.findOrNull(entityClass: KClass<T>, primaryKey: Any): T? =
    find(entityClass.java, primaryKey)

inline fun <reified T : Any> EntityManager.findOrNull(primaryKey: Any): T? =
    findOrNull(T::class, primaryKey)

fun <T : Any> EntityManager.findOrNull(
    entityClass: KClass<T>,
    primaryKey: Any,
    properties: Map<String, Any>
): T? =
    findOrNull(entityClass.java, primaryKey, properties)

inline fun <reified T : Any> EntityManager.findOrNull(primaryKey: Any, properties: Map<String, Any>): T? =
    findOrNull(T::class, primaryKey, properties)

fun <T : Any> EntityManager.findOrNull(entityClass: KClass<T>, primaryKey: Any, lockMode: LockModeType): T? =
    findOrNull(entityClass.java, primaryKey, lockMode)

inline fun <reified T : Any> EntityManager.findOrNull(primaryKey: Any, lockMode: LockModeType): T? =
    findOrNull(T::class, primaryKey, lockMode)

fun <T : Any> EntityManager.findOrNull(
    entityClass: KClass<T>,
    primaryKey: Any,
    lockMode: LockModeType,
    properties: Map<String, Any>
): T? =
    findOrNull(entityClass.java, primaryKey, lockMode, properties)

inline fun <reified T : Any> EntityManager.findOrNull(
    primaryKey: Any,
    lockMode: LockModeType,
    properties: Map<String, Any>
): T? =
    findOrNull(T::class, primaryKey, lockMode, properties)

fun <T : Any> EntityManager.getReference(entityClass: KClass<T>, primaryKey: Any): T =
    getReference(entityClass.java, primaryKey)

inline fun <reified T : Any> EntityManager.getReference(primaryKey: Any): T =
    getReference(T::class, primaryKey)

fun <T : Any> EntityManager.createQuery(qlString: String, resultClass: KClass<T>): TypedQuery<T> =
    createQuery(qlString, resultClass.java)

context(EntityManager)
inline fun <reified T : Any> createQuery(qlString: String): TypedQuery<T> =
    createQuery(qlString, T::class)

fun <T : Any> EntityManager.createNamedQuery(name: String, resultClass: KClass<T>): TypedQuery<T> =
    createNamedQuery(name, resultClass.java)

context(EntityManager)
inline fun <reified T : Any> createNamedQuery(name: String): TypedQuery<T> =
    createNamedQuery(name, T::class)

fun <T : Any> EntityManager.createEntityGraph(rootType: KClass<T>): EntityGraph<T> =
    createEntityGraph(rootType.java)

inline fun <reified T : Any> EntityManager.createEntityGraph(): EntityGraph<T> =
    createEntityGraph(T::class)

fun <T : Any> EntityManager.getEntityGraphs(entityClass: KClass<T>): List<EntityGraph<in T>> =
    getEntityGraphs(entityClass.java)

inline fun <reified T : Any> EntityManager.getEntityGraphs(): List<EntityGraph<in T>> =
    getEntityGraphs(T::class)

fun <T : Any> EntityManager.getReferenceOrNull(entityClass: KClass<T>, primaryKey: Any): T? =
    getReferenceOrNull(entityClass.java, primaryKey)

inline fun <reified T : Any> EntityManager.getReferenceOrNull(primaryKey: Any): T? =
    getReferenceOrNull(T::class, primaryKey)
