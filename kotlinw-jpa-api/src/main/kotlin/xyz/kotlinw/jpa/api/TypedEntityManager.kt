package xyz.kotlinw.jpa.api

import jakarta.persistence.EntityGraph
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.EntityTransaction
import jakarta.persistence.FlushModeType
import jakarta.persistence.LockModeType
import jakarta.persistence.Query
import jakarta.persistence.StoredProcedureQuery
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaDelete
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.CriteriaUpdate
import jakarta.persistence.metamodel.Metamodel

interface TypedEntityManager : EntityManager {

    @Deprecated(
        message = "Use `persistEntity()` instead which enforces an existing compile-time transactional context.",
        replaceWith = ReplaceWith("this.persistEntity(entity)")
    )
    override fun persist(entity: Any)

    /**
     * @see [EntityManager.persist]
     */
    context(Transactional)
    fun <T : Any> persistEntity(entity: T): T

    @Deprecated(
        message = "Use `mergeEntity()` instead which enforces an existing compile-time transactional context.",
        replaceWith = ReplaceWith("this.mergeEntity(entity)")
    )
    override fun <T : Any> merge(entity: T): T

    /**
     * @see [EntityManager.merge]
     */
    context(Transactional)
    fun <T : Any> mergeEntity(entity: T): T

    @Deprecated(
        message = "Use `removeEntity()` instead which enforces an existing compile-time transactional context.",
        replaceWith = ReplaceWith("this.removeEntity(entity)")
    )
    override fun remove(entity: Any)

    context(Transactional)
    fun removeEntity(entity: Any)

    @Deprecated(
        message = "Use `findOrNull()` instead which has a better naming convention.",
        replaceWith = ReplaceWith(
            "this.findOrNull(entityClass, primaryKey)",
            imports = ["xyz.kotlinw.jpa.api.findOrNull"]
        )
    )
    override fun <T : Any> find(entityClass: Class<T>, primaryKey: Any): T?

    @Deprecated(
        message = "Use `findOrNull()` instead which has a better naming convention.",
        replaceWith = ReplaceWith(
            "this.findOrNull(entityClass, primaryKey, properties)",
            imports = ["xyz.kotlinw.jpa.api.findOrNull"]
        )
    )
    override fun <T : Any> find(entityClass: Class<T>, primaryKey: Any, properties: Map<String, Any>): T?

    @Deprecated(
        message = "Use `findOrNull()` instead which has a better naming convention.",
        replaceWith = ReplaceWith(
            "this.findOrNull(entityClass, primaryKey, lockMode)",
            imports = ["xyz.kotlinw.jpa.api.findOrNull"]
        )
    )
    override fun <T : Any> find(entityClass: Class<T>, primaryKey: Any, lockMode: LockModeType): T?

    @Deprecated(
        message = "Use `findOrNull()` instead which has a better naming convention.",
        replaceWith = ReplaceWith(
            "this.findOrNull(entityClass, primaryKey, lockMode, properties",
            imports = ["xyz.kotlinw.jpa.api.findOrNull"]
        )
    )
    override fun <T : Any> find(
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

    @Deprecated(
        message = "Use the type-safe query creation functions.",
        replaceWith = ReplaceWith(
            "this.createTypedQuery(qlString, TODO())",
            imports = ["xyz.kotlinw.jpa.core.createTypedQuery"]
        )
    )
    override fun createQuery(qlString: String): Query

    override fun <T : Any> createQuery(criteriaQuery: CriteriaQuery<T>): TypedQuery<T>

    override fun createQuery(updateQuery: CriteriaUpdate<*>): Query

    override fun createQuery(deleteQuery: CriteriaDelete<*>): Query

    @Deprecated(
        message = "Use the type-safe query creation functions.",
        replaceWith = ReplaceWith(
            "this.createTypedQuery(qlString, resultClass)",
            imports = ["xyz.kotlinw.jpa.core.createTypedQuery"]
        )
    )
    override fun <T : Any> createQuery(qlString: String, resultClass: Class<T>): TypedQuery<T>

    @Deprecated(
        message = "Use the type-safe query creation functions.",
        replaceWith = ReplaceWith(
            "this.createTypedNamedQuery(name, TODO())",
            imports = ["xyz.kotlinw.jpa.core.createTypedNamedQuery"]
        )
    )
    override fun createNamedQuery(name: String): Query

    @Deprecated(
        message = "Use the type-safe query creation functions.",
        replaceWith = ReplaceWith(
            "this.createTypedNamedQuery(name, resultClass)",
            imports = ["xyz.kotlinw.jpa.core.createTypedNamedQuery"]
        )
    )
    override fun <T : Any> createNamedQuery(name: String, resultClass: Class<T>): TypedQuery<T>

    override fun createNativeQuery(sqlString: String): Query

    override fun createNativeQuery(sqlString: String, resultClass: Class<*>): Query

    override fun createNativeQuery(sqlString: String, resultSetMapping: String): Query

    override fun createNamedStoredProcedureQuery(name: String): StoredProcedureQuery

    override fun createStoredProcedureQuery(procedureName: String): StoredProcedureQuery

    override fun createStoredProcedureQuery(procedureName: String, vararg resultClasses: Class<*>): StoredProcedureQuery

    override fun createStoredProcedureQuery(
        procedureName: String,
        vararg resultSetMappings: String
    ): StoredProcedureQuery

    override fun joinTransaction()

    override fun isJoinedToTransaction(): Boolean

    override fun <T : Any> unwrap(cls: Class<T>): T

    @Deprecated(message = "Use `unwrap()` instead.", replaceWith = ReplaceWith("this.unwrap()"))
    override fun getDelegate(): Any

    override fun close()

    override fun isOpen(): Boolean

    override fun getTransaction(): EntityTransaction

    override fun getEntityManagerFactory(): EntityManagerFactory

    override fun getCriteriaBuilder(): CriteriaBuilder

    override fun getMetamodel(): Metamodel

    override fun <T : Any> createEntityGraph(rootType: Class<T>): EntityGraph<T>

    override fun createEntityGraph(graphName: String): EntityGraph<*>?

    override fun getEntityGraph(graphName: String): EntityGraph<*>

    override fun <T : Any> getEntityGraphs(entityClass: Class<T>): List<EntityGraph<in T>>
}
