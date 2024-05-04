package kotlinw.hibernate.core.api

import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import jakarta.persistence.criteria.CriteriaQuery

@JvmInline
internal value class TypeSafeEntityManagerImpl(private val delegate: EntityManager) : EntityManager by delegate,
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

    override fun <T : Any> findOrNull(entityClass: Class<T>, primaryKey: Any): T? =
        find(entityClass, primaryKey)

    override fun <T : Any> findOrNull(entityClass: Class<T>, primaryKey: Any, properties: Map<String, Any>): T? =
        find(entityClass, primaryKey, properties)

    override fun <T : Any> findOrNull(entityClass: Class<T>, primaryKey: Any, lockMode: LockModeType): T? =
        find(entityClass, primaryKey, lockMode)

    override fun <T : Any> findOrNull(
        entityClass: Class<T>,
        primaryKey: Any,
        lockMode: LockModeType,
        properties: Map<String, Any>
    ): T? =
        find(entityClass, primaryKey, lockMode, properties)

    override fun <T : Any> createQuery(criteriaQuery: CriteriaQuery<T>): TypeSafeQuery<T> =
        TypeSafeQueryImpl(delegate.createQuery(criteriaQuery))

    override fun <T : Any> createQuery(qlString: String, resultClass: Class<T>): TypeSafeQuery<T> =
        TypeSafeQueryImpl(delegate.createQuery(qlString, resultClass))

    override fun <T : Any> createNamedQuery(name: String, resultClass: Class<T>): TypeSafeQuery<T> =
        TypeSafeQueryImpl(delegate.createNamedQuery(name, resultClass))
}
