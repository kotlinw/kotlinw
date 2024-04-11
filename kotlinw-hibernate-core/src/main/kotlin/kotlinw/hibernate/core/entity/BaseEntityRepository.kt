package kotlinw.hibernate.core.entity

import kotlin.reflect.KClass
import kotlinw.hibernate.core.api.JpaSessionContext
import kotlinw.hibernate.core.api.Transactional

interface BaseEntityRepository<E : BaseEntity> {

    context(JpaSessionContext)
    fun findAll(): List<E>

    context(Transactional, JpaSessionContext)
    fun persist(entity: E): E
}

abstract class BaseEntityRepositoryImpl<E : BaseEntity>(
    private val entityClass: KClass<E>
) : BaseEntityRepository<E> {

    protected val entityName = entityClass.simpleName!!

    context(JpaSessionContext)
    override fun findAll(): List<E> = query("FROM ${entityClass.simpleName}", entityClass)

    context(Transactional, JpaSessionContext)
    override fun persist(entity: E): E = entityManager.persistEntity(entity)

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
