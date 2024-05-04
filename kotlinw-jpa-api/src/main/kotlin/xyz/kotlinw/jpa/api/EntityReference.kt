package xyz.kotlinw.jpa.api

import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import java.io.Serializable
import kotlin.reflect.KClass

data class EntityReference<E : Any, ID : Serializable>(
    val entityClass: KClass<E>,
    val id: ID
)

fun <E : Any, ID : Serializable> EntityManager.getReferenceOrNull(entityReference: EntityReference<E, ID>): E? =
    getReferenceOrNull(entityReference.entityClass, entityReference.id)

fun <E : Any, ID : Serializable> EntityManager.getReference(entityReference: EntityReference<E, ID>): E =
    getReference(entityReference.entityClass, entityReference.id)

fun <E : Any, ID : Serializable> EntityManager.findOrNull(entityReference: EntityReference<E, ID>): E? =
    findOrNull(entityReference.entityClass, entityReference.id)

fun <E : Any, ID : Serializable> EntityManager.findOrNull(
    entityReference: EntityReference<E, ID>,
    properties: Map<String, Any>
): E? =
    findOrNull(entityReference.entityClass, entityReference.id, properties)

fun <E : Any, ID : Serializable> EntityManager.findOrNull(
    entityReference: EntityReference<E, ID>,
    lockMode: LockModeType
): E? =
    findOrNull(entityReference.entityClass, entityReference.id, lockMode)

fun <E : Any, ID : Serializable> EntityManager.findOrNull(
    entityReference: EntityReference<E, ID>,
    lockMode: LockModeType,
    properties: Map<String, Any>
): E? =
    findOrNull(entityReference.entityClass, entityReference.id, lockMode, properties)
