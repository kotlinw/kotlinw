package kotlinw.hibernate.core.api

import jakarta.persistence.EntityGraph
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.EntityTransaction
import jakarta.persistence.FlushModeType
import jakarta.persistence.LockModeType
import jakarta.persistence.Query
import jakarta.persistence.StoredProcedureQuery
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaDelete
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.CriteriaUpdate
import jakarta.persistence.metamodel.Metamodel
import kotlinw.hibernate.core.annotation.NotTypeSafeJpaApi


interface TypeSafeEntityManager : EntityManager {

    @Deprecated(
        message = "Use `persistEntity()` instead which enforces an existing compile-time transactional context.",
        replaceWith = ReplaceWith("persistEntity(entity)")
    )
    override fun persist(entity: Any)

    /**
     * @see [EntityManager.persist]
     */
    context(TransactionContext)
    fun <T : Any> persistEntity(entity: T): T

    @Deprecated(
        message = "Use `mergeEntity()` instead which enforces an existing compile-time transactional context.",
        replaceWith = ReplaceWith("mergeEntity(entity)")
    )
    override fun <T : Any> merge(entity: T): T

    /**
     * @see [EntityManager.merge]
     */
    context(TransactionContext)
    fun <T : Any> mergeEntity(entity: T): T

    @Deprecated(
        message = "Use `removeEntity()` instead which enforces an existing compile-time transactional context.",
        replaceWith = ReplaceWith("removeEntity(entity)")
    )
    override fun remove(entity: Any)

    context(TransactionContext)
    fun removeEntity(entity: Any)

    @Deprecated(
        message = "Use `findOrNull()` instead which has a better naming convention.",
        replaceWith = ReplaceWith("findOrNull(entityClass, primaryKey)")
    )
    override fun <T : Any> find(entityClass: Class<T>, primaryKey: Any): T?

    fun <T : Any> findOrNull(entityClass: Class<T>, primaryKey: Any): T?

    @Deprecated(
        message = "Use `findOrNull()` instead which has a better naming convention.",
        replaceWith = ReplaceWith("findOrNull(entityClass, primaryKey, properties)")
    )
    override fun <T : Any> find(entityClass: Class<T>, primaryKey: Any, properties: Map<String, Any>): T?

    fun <T : Any> findOrNull(entityClass: Class<T>, primaryKey: Any, properties: Map<String, Any>): T?

    @Deprecated(
        message = "Use `findOrNull()` instead which has a better naming convention.",
        replaceWith = ReplaceWith("findOrNull(entityClass, primaryKey, lockMode)")
    )
    override fun <T : Any> find(entityClass: Class<T>, primaryKey: Any, lockMode: LockModeType): T?

    fun <T : Any> findOrNull(entityClass: Class<T>, primaryKey: Any, lockMode: LockModeType): T?

    @Deprecated(
        message = "Use `findOrNull()` instead which has a better naming convention.",
        replaceWith = ReplaceWith("findOrNull(entityClass, primaryKey, lockMode, properties")
    )
    override fun <T : Any> find(
        entityClass: Class<T>,
        primaryKey: Any,
        lockMode: LockModeType,
        properties: Map<String, Any>
    ): T?

    fun <T : Any> findOrNull(
        entityClass: Class<T>,
        primaryKey: Any,
        lockMode: LockModeType,
        properties: Map<String, Any>
    ): T?

    override fun <T : Any> getReference(entityClass: Class<T>, primaryKey: Any): T

    override fun flush()

    override fun getFlushMode(): FlushModeType

    override fun setFlushMode(flushMode: FlushModeType)

    override fun lock(entity: Any, lockMode: LockModeType)

    override fun lock(entity: Any, lockMode: LockModeType, properties: Map<String, Any>)

    override fun refresh(entity: Any)

    override fun refresh(entity: Any, properties: Map<String, Any>)

    override fun refresh(entity: Any, lockMode: LockModeType)

    override fun refresh(entity: Any, lockMode: LockModeType, properties: Map<String, Any>)

    override fun clear()

    override fun detach(entity: Any)

    override operator fun contains(entity: Any): Boolean

    override fun getLockMode(entity: Any): LockModeType

    override fun setProperty(propertyName: String, value: Any)

    override fun getProperties(): MutableMap<String, Any>

    @NotTypeSafeJpaApi
    override fun createQuery(qlString: String): Query

    override fun <T : Any> createQuery(criteriaQuery: CriteriaQuery<T>): TypedQuery<T>

    @NotTypeSafeJpaApi
    override fun createQuery(updateQuery: CriteriaUpdate<*>): Query

    @NotTypeSafeJpaApi
    override fun createQuery(deleteQuery: CriteriaDelete<*>): Query

    override fun <T : Any> createQuery(qlString: String, resultClass: Class<T>): TypedQuery<T>

    @NotTypeSafeJpaApi
    override fun createNamedQuery(name: String): Query

    override fun <T> createNamedQuery(name: String, resultClass: Class<T>): TypedQuery<T>

    @NotTypeSafeJpaApi
    override fun createNativeQuery(sqlString: String): Query

    override fun createNativeQuery(sqlString: String, resultClass: Class<*>): Query

    @NotTypeSafeJpaApi
    override fun createNativeQuery(sqlString: String, resultSetMapping: String): Query

    @NotTypeSafeJpaApi
    override fun createNamedStoredProcedureQuery(name: String): StoredProcedureQuery

    @NotTypeSafeJpaApi
    override fun createStoredProcedureQuery(procedureName: String): StoredProcedureQuery

    @NotTypeSafeJpaApi
    override fun createStoredProcedureQuery(procedureName: String, vararg resultClasses: Class<*>): StoredProcedureQuery

    @NotTypeSafeJpaApi
    override fun createStoredProcedureQuery(
        procedureName: String,
        vararg resultSetMappings: String
    ): StoredProcedureQuery

    override fun joinTransaction()

    override fun isJoinedToTransaction(): Boolean

    override fun <T : Any> unwrap(cls: Class<T>): T

    @Deprecated(message = "Use `unwrap()` instead.", replaceWith = ReplaceWith("unwrap()"))
    override fun getDelegate(): Any

    override fun close()

    override fun isOpen(): Boolean

    override fun getTransaction(): EntityTransaction

    override fun getEntityManagerFactory(): EntityManagerFactory

    override fun getCriteriaBuilder(): CriteriaBuilder

    override fun getMetamodel(): Metamodel

    override fun <T : Any> createEntityGraph(rootType: Class<T>): EntityGraph<T>

    override fun createEntityGraph(graphName: String): EntityGraph<*>

    override fun getEntityGraph(graphName: String): EntityGraph<*>

    override fun <T : Any> getEntityGraphs(entityClass: Class<T>): List<EntityGraph<in T>>
}

internal class TypeSafeEntityManagerImpl(private val delegate: EntityManager) : EntityManager by delegate,
    TypeSafeEntityManager {

    context(TransactionContext)
    override fun <T : Any> persistEntity(entity: T): T {
        persist(entity)
        return entity
    }

    context(TransactionContext)
    override fun <T : Any> mergeEntity(entity: T): T =
        merge(entity)

    context(TransactionContext)
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
}
