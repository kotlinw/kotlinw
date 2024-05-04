package xyz.kotlinw.jpa.internal

import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaQuery
import xyz.kotlinw.jpa.api.Transactional
import xyz.kotlinw.jpa.api.TypedEntityManager
import xyz.kotlinw.jpa.api.TypedQuery

@JvmInline
value class TypedEntityManagerImpl(private val delegate: EntityManager) : EntityManager by delegate,
    TypedEntityManager {

    context(Transactional)
    override fun <T : Any> persistEntity(entity: T): T {
        persist(entity)
        return entity
    }

    context(Transactional)
    override fun <T : Any> mergeEntity(entity: T): T =
        merge(entity)

    context(Transactional)
    override fun removeEntity(entity: Any) {
        remove(entity)
    }

    override fun <T : Any> createQuery(criteriaQuery: CriteriaQuery<T>): TypedQuery<T> =
        TypedQueryImpl(delegate.createQuery(criteriaQuery))

    override fun <T : Any> createQuery(qlString: String, resultClass: Class<T>): TypedQuery<T> =
        TypedQueryImpl(delegate.createQuery(qlString, resultClass))

    override fun <T : Any> createNamedQuery(name: String, resultClass: Class<T>): TypedQuery<T> =
        TypedQueryImpl(delegate.createNamedQuery(name, resultClass))
}
