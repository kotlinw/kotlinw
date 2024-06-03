package xyz.kotlinw.jpa.repository

import jakarta.persistence.LockModeType
import java.io.Serializable
import kotlinx.coroutines.flow.Flow
import xyz.kotlinw.jpa.api.JpaSessionContext
import xyz.kotlinw.jpa.api.ReactiveJpaContext
import xyz.kotlinw.jpa.api.Transactional

interface GenericEntityRepository<E, ID : Serializable> {

    context(Transactional, JpaSessionContext)
    fun persist(entity: E): E

    context(Transactional, JpaSessionContext)
    fun merge(entity: E): E

    context(Transactional, JpaSessionContext)
    fun remove(entity: E)

    context(JpaSessionContext)
    fun findOrNull(id: ID): E?

    context(JpaSessionContext)
    fun findOrNull(id: ID, properties: Map<String, Any>): E?

    context(JpaSessionContext)
    fun findOrNull(id: ID, lockMode: LockModeType): E?

    context(JpaSessionContext)
    fun findOrNull(id: ID, lockMode: LockModeType, properties: Map<String, Any>): E?

    context(JpaSessionContext)
    fun getReference(id: ID): E

    context(JpaSessionContext)
    fun getReferenceOrNull(id: ID): E?

    context(JpaSessionContext)
    fun findAll(): List<E>

    context(ReactiveJpaContext)
    fun subscribeFindAll(): Flow<List<E>>
}
