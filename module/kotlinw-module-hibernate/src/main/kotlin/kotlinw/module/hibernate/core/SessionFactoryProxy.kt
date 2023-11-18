package kotlinw.module.hibernate.core

import jakarta.persistence.EntityGraph
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceUnitUtil
import jakarta.persistence.Query
import jakarta.persistence.SynchronizationType
import jakarta.persistence.metamodel.Metamodel
import javax.naming.Reference
import kotlinx.atomicfu.atomic
import org.hibernate.Cache
import org.hibernate.Session
import org.hibernate.SessionBuilder
import org.hibernate.SessionFactory
import org.hibernate.StatelessSession
import org.hibernate.StatelessSessionBuilder
import org.hibernate.boot.registry.BootstrapServiceRegistry
import org.hibernate.boot.spi.SessionFactoryOptions
import org.hibernate.engine.spi.FilterDefinition
import org.hibernate.graph.RootGraph
import org.hibernate.query.criteria.HibernateCriteriaBuilder
import org.hibernate.relational.SchemaManager
import org.hibernate.stat.Statistics
import java.sql.Connection

class SessionFactoryProxy : SessionFactory {

    private val delegateHolder = atomic<SessionFactory?>(null)

    private val delegate
        get() = delegateHolder.value ?: throw IllegalStateException("Delegate of $this is not initialized yet.")

    fun initialize(delegate: SessionFactory) {
        check(delegateHolder.value == null)
        delegateHolder.value = delegate
    }

    override fun close() = delegate.close()

    override fun createEntityManager(): EntityManager = delegate.createEntityManager()

    override fun createEntityManager(map: MutableMap<Any?, Any?>?): EntityManager = delegate.createEntityManager(map)

    override fun createEntityManager(synchronizationType: SynchronizationType?): EntityManager =
        delegate.createEntityManager(synchronizationType)

    override fun createEntityManager(
        synchronizationType: SynchronizationType?,
        map: MutableMap<Any?, Any?>?
    ): EntityManager = delegate.createEntityManager(synchronizationType, map)

    override fun getCriteriaBuilder(): HibernateCriteriaBuilder = delegate.criteriaBuilder

    override fun getMetamodel(): Metamodel = delegate.metamodel

    override fun isOpen(): Boolean = delegate.isOpen

    override fun getProperties(): MutableMap<String, Any> = delegate.properties

    override fun getCache(): Cache = delegate.cache

    override fun getPersistenceUnitUtil(): PersistenceUnitUtil = delegate.persistenceUnitUtil

    override fun addNamedQuery(name: String?, query: Query?) = delegate.addNamedQuery(name, query)

    override fun <T : Any?> unwrap(cls: Class<T>?): T = delegate.unwrap(cls)

    override fun <T : Any?> addNamedEntityGraph(graphName: String?, entityGraph: EntityGraph<T>?) =
        delegate.addNamedEntityGraph(graphName, entityGraph)

    override fun getReference(): Reference = delegate.reference

    override fun withOptions(): SessionBuilder = delegate.withOptions()

    override fun openSession(): Session = delegate.openSession()

    override fun getCurrentSession(): Session = delegate.currentSession

    override fun withStatelessOptions(): StatelessSessionBuilder = delegate.withStatelessOptions()

    override fun openStatelessSession(): StatelessSession = delegate.openStatelessSession()

    override fun openStatelessSession(connection: Connection?): StatelessSession =
        delegate.openStatelessSession(connection)

    override fun getStatistics(): Statistics = delegate.statistics

    override fun getSchemaManager(): SchemaManager = delegate.schemaManager

    override fun isClosed(): Boolean = delegate.isClosed

    override fun <T : Any?> findEntityGraphsByType(entityClass: Class<T>?): MutableList<EntityGraph<in T>> =
        delegate.findEntityGraphsByType(entityClass)

    override fun findEntityGraphByName(name: String?): RootGraph<*> = delegate.findEntityGraphByName(name)

    override fun getDefinedFilterNames(): MutableSet<String> = delegate.definedFilterNames

    override fun getFilterDefinition(filterName: String?): FilterDefinition = delegate.getFilterDefinition(filterName)

    override fun getDefinedFetchProfileNames(): MutableSet<String> = delegate.definedFetchProfileNames

    override fun getSessionFactoryOptions(): SessionFactoryOptions = delegate.sessionFactoryOptions
}
