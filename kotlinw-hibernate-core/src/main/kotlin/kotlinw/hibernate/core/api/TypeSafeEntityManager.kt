package kotlinw.hibernate.core.api

import jakarta.persistence.EntityGraph
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.EntityNotFoundException
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
import kotlin.reflect.KClass
import kotlinw.hibernate.core.annotation.NotTypeSafeJpaApi

sealed interface TypeSafeEntityManager : EntityManager {

    @Deprecated(
        message = "Use `persistEntity()` instead which enforces an existing compile-time transactional context.",
        level = DeprecationLevel.ERROR,
        replaceWith = ReplaceWith("persistEntity(entity)")
    )
    override fun persist(entity: Any)

    /**
     * @see [EntityManager.persist]
     */
    context(Transactional)
    fun <T : Any> persistEntity(entity: T): T

    @Deprecated(
        message = "Use `mergeEntity()` instead which enforces an existing compile-time transactional context.",
        level = DeprecationLevel.ERROR,
        replaceWith = ReplaceWith("mergeEntity(entity)")
    )
    override fun <T : Any> merge(entity: T): T

    /**
     * @see [EntityManager.merge]
     */
    context(Transactional)
    fun <T : Any> mergeEntity(entity: T): T

    @Deprecated(
        message = "Use `removeEntity()` instead which enforces an existing compile-time transactional context.",
        level = DeprecationLevel.ERROR,
        replaceWith = ReplaceWith("removeEntity(entity)")
    )
    override fun remove(entity: Any)

    context(Transactional)
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

    override fun <T : Any> createQuery(criteriaQuery: CriteriaQuery<T>): TypeSafeQuery<T>

    @NotTypeSafeJpaApi
    override fun createQuery(updateQuery: CriteriaUpdate<*>): Query

    @NotTypeSafeJpaApi
    override fun createQuery(deleteQuery: CriteriaDelete<*>): Query

    override fun <T : Any> createQuery(qlString: String, resultClass: Class<T>): TypeSafeQuery<T>

    @NotTypeSafeJpaApi
    override fun createNamedQuery(name: String): Query

    override fun <T : Any> createNamedQuery(name: String, resultClass: Class<T>): TypeSafeQuery<T>

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

    @NotTypeSafeJpaApi
    override fun createEntityGraph(graphName: String): EntityGraph<*>

    override fun getEntityGraph(graphName: String): EntityGraph<*>

    override fun <T : Any> getEntityGraphs(entityClass: Class<T>): List<EntityGraph<in T>>
}

//
// EntityManager extensions
//

fun <T : Any> TypeSafeEntityManager.getReferenceOrNull(entityClass: Class<T>, primaryKey: Any): T? =
    try {
        getReference(entityClass, primaryKey)
    } catch (e: EntityNotFoundException) {
        null
    }

//
// KClass<T> extensions for Class<T> usages
//

fun <T : Any> TypeSafeEntityManager.findOrNull(entityClass: KClass<T>, primaryKey: Any): T? =
    findOrNull(entityClass.java, primaryKey)

inline fun <reified T : Any> TypeSafeEntityManager.findOrNull(primaryKey: Any): T? =
    findOrNull(T::class.java, primaryKey)

fun <T : Any> TypeSafeEntityManager.findOrNull(
    entityClass: KClass<T>,
    primaryKey: Any,
    properties: Map<String, Any>
): T? =
    findOrNull(entityClass.java, primaryKey, properties)

inline fun <reified T : Any> TypeSafeEntityManager.findOrNull(
    primaryKey: Any,
    properties: Map<String, Any>
): T? =
    findOrNull(T::class, primaryKey, properties)

fun <T : Any> TypeSafeEntityManager.findOrNull(entityClass: KClass<T>, primaryKey: Any, lockMode: LockModeType): T? =
    findOrNull(entityClass.java, primaryKey, lockMode)

inline fun <reified T : Any> TypeSafeEntityManager.findOrNull(primaryKey: Any, lockMode: LockModeType): T? =
    findOrNull(T::class, primaryKey, lockMode)

fun <T : Any> TypeSafeEntityManager.findOrNull(
    entityClass: KClass<T>,
    primaryKey: Any,
    lockMode: LockModeType,
    properties: Map<String, Any>
): T? =
    findOrNull(entityClass.java, primaryKey, lockMode, properties)

inline fun <reified T : Any> TypeSafeEntityManager.findOrNull(
    primaryKey: Any,
    lockMode: LockModeType,
    properties: Map<String, Any>
): T? =
    findOrNull(T::class, primaryKey, lockMode, properties)

fun <T : Any> TypeSafeEntityManager.getReference(entityClass: KClass<T>, primaryKey: Any): T =
    getReference(entityClass.java, primaryKey)

inline fun <reified T : Any> TypeSafeEntityManager.getReference(primaryKey: Any): T =
    getReference(T::class, primaryKey)

fun <T : Any> TypeSafeEntityManager.createQuery(qlString: String, resultClass: KClass<T>): TypeSafeQuery<T> =
    createQuery(qlString, resultClass.java)

inline fun <reified T : Any> TypeSafeEntityManager.createQuery(qlString: String): TypeSafeQuery<T> =
    createQuery(qlString, T::class)

fun <T : Any> TypeSafeEntityManager.createNamedQuery(name: String, resultClass: KClass<T>): TypeSafeQuery<T> =
    createNamedQuery(name, resultClass.java)

inline fun <reified T : Any> TypeSafeEntityManager.createNamedQuery(name: String): TypeSafeQuery<T> =
    createNamedQuery(name, T::class)

fun <T : Any> TypeSafeEntityManager.createEntityGraph(rootType: KClass<T>): EntityGraph<T> =
    createEntityGraph(rootType.java)

inline fun <reified T : Any> TypeSafeEntityManager.createEntityGraph(): EntityGraph<T> =
    createEntityGraph(T::class)

fun <T : Any> TypeSafeEntityManager.getEntityGraphs(entityClass: KClass<T>): List<EntityGraph<in T>> =
    getEntityGraphs(entityClass.java)

inline fun <reified T : Any> TypeSafeEntityManager.getEntityGraphs(): List<EntityGraph<in T>> =
    getEntityGraphs(T::class)

fun <T : Any> TypeSafeEntityManager.getReferenceOrNull(entityClass: KClass<T>, primaryKey: Any): T? =
    getReferenceOrNull(entityClass.java, primaryKey)

inline fun <reified T : Any> TypeSafeEntityManager.getReferenceOrNull(primaryKey: Any): T? =
    getReferenceOrNull(T::class, primaryKey)
