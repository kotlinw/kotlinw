package kotlinw.hibernate.core.entity

import jakarta.persistence.LockModeType
import java.io.Serializable
import kotlin.reflect.KClass
import kotlinw.hibernate.core.api.JpaSessionContext
import kotlinw.hibernate.core.api.Transactional
import kotlinw.hibernate.core.api.createTypeSafeQuery
import kotlinw.hibernate.core.api.findOrNull
import kotlinw.hibernate.core.api.getReference
import kotlinw.hibernate.core.api.getSingleResultOrNull
import kotlinw.hibernate.core.api.query

interface EntityRepository<E : AbstractEntity<ID>, ID : Serializable> {

    context(Transactional, JpaSessionContext)
    fun persist(entity: E): E

    context(Transactional, JpaSessionContext)
    fun mergeEntity(entity: E): E

    context(Transactional, JpaSessionContext)
    fun removeEntity(entity: E)

    context(JpaSessionContext)
    fun findOrNull(id: ID): E?

    context(JpaSessionContext)
    fun findOrNull(id: ID, properties: Map<String, Any>): E?

    context(JpaSessionContext)
    fun findOrNull(id: ID, lockMode: LockModeType): E?

    context(JpaSessionContext)
    fun findOrNull(
        id: ID,
        lockMode: LockModeType,
        properties: Map<String, Any>
    ): E?

    context(JpaSessionContext)
    fun getReference(id: ID): E

    context(JpaSessionContext)
    fun getReferenceOrNull(id: ID): E?

    context(JpaSessionContext)
    fun findAll(): List<E>
}

interface SimpleBaseEntityRepository<E : SimpleBaseEntity> : EntityRepository<E, BaseEntityId>

interface BaseEntityRepository<E : BaseEntity> : SimpleBaseEntityRepository<E>

abstract class EntityRepositoryImpl<E : AbstractEntity<ID>, ID : Serializable>(
    protected val entityClass: KClass<E>
) : EntityRepository<E, ID> {

    protected val entityName =
        entityClass.simpleName!! // TODO ez lehet más is, a persistence provider-től kellene lekérdezni

    context(JpaSessionContext)
    override fun findAll(): List<E> = query("FROM $entityName", entityClass)

    context(Transactional, JpaSessionContext)
    override fun persist(entity: E): E = entityManager.persistEntity(entity)

    context(Transactional, JpaSessionContext)
    override fun mergeEntity(entity: E): E = entityManager.mergeEntity(entity)

    context(Transactional, JpaSessionContext)
    override fun removeEntity(entity: E) = entityManager.removeEntity(entity)

    context(JpaSessionContext)
    override fun findOrNull(id: ID): E? = entityManager.findOrNull(entityClass, id)

    context(JpaSessionContext)
    override fun findOrNull(id: ID, properties: Map<String, Any>): E? =
        entityManager.findOrNull(entityClass, id, properties)

    context(JpaSessionContext)
    override fun findOrNull(id: ID, lockMode: LockModeType): E? = entityManager.findOrNull(entityClass, id, lockMode)

    context(JpaSessionContext)
    override fun findOrNull(
        id: ID,
        lockMode: LockModeType,
        properties: Map<String, Any>
    ): E? =
        entityManager.findOrNull(entityClass, id, lockMode, properties)

    context(JpaSessionContext)
    override fun getReferenceOrNull(id: ID): E? = entityManager.findOrNull(entityClass, id)

    context(JpaSessionContext)
    override fun getReference(id: ID): E = entityManager.getReference(entityClass, id)

    //
    // Methods for subclasses
    //

    context(JpaSessionContext)
    protected fun <T : Any> query(qlQuery: String, resultType: KClass<T>, vararg arguments: Any?): List<T> =
        entityManager.query(qlQuery, resultType, *arguments)

    context(JpaSessionContext)
    protected inline fun <reified T : Any> query(qlQuery: String, vararg arguments: Any?): List<T> =
        query(qlQuery, T::class, *arguments)

    context(JpaSessionContext)
    protected fun queryEntity(qlQuery: String, vararg arguments: Any?): List<E> =
        query(qlQuery, entityClass, *arguments)

    context(JpaSessionContext)
    protected inline fun <reified T : Any> querySingleOrNull(qlQuery: String, vararg arguments: Any?): T? =
        entityManager.createTypeSafeQuery<T>(qlQuery, *arguments).getSingleResultOrNull()

    context(JpaSessionContext)
    protected inline fun <reified T : Any> querySingle(qlQuery: String, vararg arguments: Any?): T =
        querySingleOrNull<T>(qlQuery, *arguments) ?: throw IllegalStateException() // TODO specifikus hibát
}

abstract class SimpleBaseEntityRepositoryImpl<E : SimpleBaseEntity>(entityClass: KClass<E>) :
    EntityRepositoryImpl<E, BaseEntityId>(entityClass),
    SimpleBaseEntityRepository<E>

abstract class BaseEntityRepositoryImpl<E : BaseEntity>(entityClass: KClass<E>) :
    SimpleBaseEntityRepositoryImpl<E>(entityClass),
    BaseEntityRepository<E>
