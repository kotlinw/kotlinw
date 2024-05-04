package kotlinw.hibernate.core.entity

import jakarta.persistence.LockModeType
import java.io.Serializable
import kotlin.reflect.KClass
import xyz.kotlinw.jpa.api.JpaSessionContext
import xyz.kotlinw.jpa.api.Transactional
import xyz.kotlinw.jpa.api.findOrNull
import xyz.kotlinw.jpa.api.getReference
import xyz.kotlinw.jpa.api.getSingleResultOrNull
import xyz.kotlinw.jpa.core.createTypedQuery
import xyz.kotlinw.jpa.core.executeQuery

interface EntityRepository<E : AbstractEntity<ID>, ID : Serializable> {

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
    override fun merge(entity: E): E = entityManager.merge(entity)

    context(Transactional, JpaSessionContext)
    override fun remove(entity: E) = entityManager.remove(entity)

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
        entityManager.executeQuery(qlQuery, resultType, *arguments)

    context(JpaSessionContext)
    protected inline fun <reified T : Any> query(qlQuery: String, vararg arguments: Any?): List<T> =
        query(qlQuery, T::class, *arguments)

    context(JpaSessionContext)
    protected fun queryEntity(qlQuery: String, vararg arguments: Any?): List<E> =
        query(qlQuery, entityClass, *arguments)

    context(JpaSessionContext)
    protected inline fun <reified T : Any> querySingleOrNull(qlQuery: String, vararg arguments: Any?): T? =
        entityManager.createTypedQuery<T>(qlQuery, *arguments).getSingleResultOrNull()

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
