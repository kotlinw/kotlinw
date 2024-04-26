package kotlinw.hibernate.core.entity

import jakarta.persistence.EntityNotFoundException
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlinw.hibernate.core.api.JpaSessionContext
import kotlinw.hibernate.core.api.Transactional
import java.io.Serializable

interface EntityRepository<E : AbstractEntity<ID>, ID : Serializable> {

    val entityName: String

    val entityClass: KClass<E>

    context(JpaSessionContext)
    fun findAll(): List<E>

    context(Transactional, JpaSessionContext)
    fun persist(entity: E): E

    context(JpaSessionContext)
    fun findByIdOrNull(id: ID): E?
}

context(JpaSessionContext)
fun <E : AbstractEntity<ID>, ID : Serializable> EntityRepository<E, ID>.findById(id: ID): E =
    findByIdOrNull(id) ?: throw EntityNotFoundException("Entity of type '$entityName' with id=$id not found")

context(JpaSessionContext)
fun <E : AbstractEntity<ID>, ID : Serializable> EntityRepository<E, ID>.getOrNull(entityReference: EntityReference<E, ID>): E? {
    check(entityReference.entityClass.isSubclassOf(entityClass))
    return findByIdOrNull(entityReference.id)
}

context(JpaSessionContext)
fun <E : AbstractEntity<ID>, ID : Serializable> EntityRepository<E, ID>.get(entityReference: EntityReference<E, ID>): E? {
    check(entityReference.entityClass.isSubclassOf(entityClass))
    return findByIdOrNull(entityReference.id)
}

interface SimpleBaseEntityRepository<E : SimpleBaseEntity> : EntityRepository<E, BaseEntityId>

interface BaseEntityRepository<E : BaseEntity> : SimpleBaseEntityRepository<E>

abstract class EntityRepositoryImpl<E : AbstractEntity<ID>, ID : Serializable>(
    final override val entityClass: KClass<E>
) : EntityRepository<E, ID> {

    final override val entityName = entityClass.simpleName!! // TODO ez lehet más is, a persistence provider-től kellene lekérdezni

    context(JpaSessionContext)
    override fun findAll(): List<E> = query("FROM ${entityClass.simpleName}", entityClass)

    context(Transactional, JpaSessionContext)
    override fun persist(entity: E): E = entityManager.persistEntity(entity)

    context(JpaSessionContext)
    override fun findByIdOrNull(id: ID): E? =
        query("FROM $entityName WHERE id=$1", entityClass, id).firstOrNull()

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

abstract class SimpleBaseEntityRepositoryImpl<E : SimpleBaseEntity>(entityClass: KClass<E>) :
    EntityRepositoryImpl<E, BaseEntityId>(entityClass),
    SimpleBaseEntityRepository<E>

abstract class BaseEntityRepositoryImpl<E : BaseEntity>(entityClass: KClass<E>) :
    SimpleBaseEntityRepositoryImpl<E>(entityClass),
    BaseEntityRepository<E>
