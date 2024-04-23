package kotlinw.hibernate.core.entity

import jakarta.persistence.EntityNotFoundException
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlinw.hibernate.core.api.JpaSessionContext
import kotlinw.hibernate.core.api.Transactional
import java.io.Serializable

interface EntityRepository<E : AbstractEntity<ID>, ID : Serializable> {

    context(JpaSessionContext)
    fun findAll(): List<E>

    context(Transactional, JpaSessionContext)
    fun persist(entity: E): E

    context(JpaSessionContext)
    fun get(entityReference: EntityReference<E, ID>): E

    context(JpaSessionContext)
    fun findByIdOrNull(id: ID): E?

    context(JpaSessionContext)
    fun findById(id: ID): E
}

interface BaseEntityRepository<E : BaseEntity> : EntityRepository<E, BaseEntityId>

abstract class EntityRepositoryImpl<E : AbstractEntity<ID>, ID : Serializable>(
    private val entityClass: KClass<E>
) : EntityRepository<E, ID> {

    protected val entityName =
        entityClass.simpleName!! // TODO ez lehet más is, a persistence provider-től kellene lekérdezni

    context(JpaSessionContext)
    override fun findAll(): List<E> = query("FROM ${entityClass.simpleName}", entityClass)

    context(Transactional, JpaSessionContext)
    override fun persist(entity: E): E = entityManager.persistEntity(entity)

    context(JpaSessionContext)
    override fun get(entityReference: EntityReference<E, ID>): E {
        check(entityReference.entityClass.isSubclassOf(entityClass))
        return findById(entityReference.id)
    }

    context(JpaSessionContext)
    override fun findByIdOrNull(id: ID): E? =
        query("FROM $entityName WHERE id=$1", entityClass, id).firstOrNull()

    context(JpaSessionContext)
    override fun findById(id: ID): E =
        findByIdOrNull(id) ?: throw EntityNotFoundException("Entity of type '$entityClass' with id=$id not found")

    context(JpaSessionContext)
    protected fun <T : Any> query(qlQuery: String, resultType: KClass<T>, vararg arguments: Any?): List<T> {
        val query = entityManager.createQuery(qlQuery, resultType.java)
        arguments.forEachIndexed { index, value ->
            query.setParameter(index + 1, value)
        }
        return query.resultList
    }

    context(JpaSessionContext)
    protected inline fun <reified T : Any> query(qlQuery: String, vararg arguments: Any?): List<T> =
        query(qlQuery, T::class, *arguments)

    context(JpaSessionContext)
    protected inline fun <reified T : Any> singleOrNull(qlQuery: String, vararg arguments: Any?): T? =
        query<T>(qlQuery, *arguments).firstOrNull()

    context(JpaSessionContext)
    protected inline fun <reified T : Any> single(qlQuery: String, vararg arguments: Any?): T =
        singleOrNull<T>(qlQuery, *arguments) ?: throw IllegalStateException() // TODO specifikus hibát
}

abstract class BaseEntityRepositoryImpl<E : BaseEntity>(entityClass: KClass<E>) :
    EntityRepositoryImpl<E, BaseEntityId>(entityClass),
    BaseEntityRepository<E>
