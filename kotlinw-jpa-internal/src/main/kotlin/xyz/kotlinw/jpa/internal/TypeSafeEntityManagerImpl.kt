package xyz.kotlinw.jpa.internal

import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaQuery
import xyz.kotlinw.jpa.api.Transactional
import xyz.kotlinw.jpa.api.TypeSafeEntityManager
import xyz.kotlinw.jpa.api.TypeSafeQuery

@JvmInline
value class TypeSafeEntityManagerImpl(private val delegate: EntityManager) : EntityManager by delegate,
    TypeSafeEntityManager {

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

    override fun <T : Any> createQuery(criteriaQuery: CriteriaQuery<T>): TypeSafeQuery<T> =
        TypeSafeQueryImpl(delegate.createQuery(criteriaQuery))

    override fun <T : Any> createQuery(qlString: String, resultClass: Class<T>): TypeSafeQuery<T> =
        TypeSafeQueryImpl(delegate.createQuery(qlString, resultClass))

    override fun <T : Any> createNamedQuery(name: String, resultClass: Class<T>): TypeSafeQuery<T> =
        TypeSafeQueryImpl(delegate.createNamedQuery(name, resultClass))
}
