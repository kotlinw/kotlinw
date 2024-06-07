package kotlinw.hibernate.core.service

import jakarta.persistence.EntityManager
import java.sql.Connection
import java.util.function.Function
import kotlinw.hibernate.core.api.asHibernateSession
import kotlinw.hibernate.core.api.runJdbcTask
import kotlinw.hibernate.core.service.JpaPersistenceService.ListenerRemovalHandle
import kotlinw.util.stdlib.collection.ConcurrentLinkedQueue
import org.hibernate.SessionFactory
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.internal.TransactionManagement
import org.hibernate.resource.transaction.spi.TransactionObserver
import xyz.kotlinw.jpa.api.JpaSessionContext
import xyz.kotlinw.jpa.api.TransactionContext
import xyz.kotlinw.jpa.api.TypedEntityManager
import xyz.kotlinw.jpa.core.asTypedEntityManager
import xyz.kotlinw.jpa.internal.JpaSessionContextImpl

interface JpaPersistenceService {

    val sessionFactory: SessionFactory // TODO ehelyett inkább specifikus metódusok kellenének, pl. listener-ek regisztrálására

    fun <T> runJpaTask(block: JpaSessionContext.() -> T): T

    fun <T> runTransactionalJpaTask(block: context(TransactionContext) JpaSessionContext.() -> T): T

    fun <T> runJdbcTask(block: Connection.() -> T): T

    fun <T> runTransactionalJdbcTask(block: context(TransactionContext)  Connection.() -> T): T

    fun interface ListenerRemovalHandle {

        fun remove()
    }

    fun addGlobalTransactionListener(listener: GlobalTransactionListener): ListenerRemovalHandle
}

interface GlobalTransactionListener {

    fun afterBegin(session: SharedSessionContractImplementor)

    fun beforeCompletion(session: SharedSessionContractImplementor)

    fun afterCompletion(session: SharedSessionContractImplementor, successful: Boolean, delayed: Boolean)

    fun afterDispose(session: SharedSessionContractImplementor)
}

class JpaPersistenceServiceImpl(
    override val sessionFactory: SessionFactory
) : JpaPersistenceService {

    private val globalTransactionListeners: MutableCollection<GlobalTransactionListener> = ConcurrentLinkedQueue()

    override fun addGlobalTransactionListener(listener: GlobalTransactionListener): ListenerRemovalHandle {
        require(!globalTransactionListeners.contains(listener)) { "Listener is already added: $listener" }
        globalTransactionListeners.add(listener)
        return ListenerRemovalHandle {
            globalTransactionListeners.remove(listener)
        }
    }

    private fun createTypedEntityManager(): TypedEntityManager =
        sessionFactory.createEntityManager().asTypedEntityManager()

    override fun <T> runJpaTask(block: JpaSessionContext.() -> T): T =
        createTypedEntityManager().use {
            block(JpaSessionContextImpl(it))
        }

    override fun <T> runTransactionalJpaTask(block: context(TransactionContext) JpaSessionContext.() -> T): T =
        runJpaTask {
            transactional {
                block(this, this@runJpaTask)
            }
        }

    override fun <T> runJdbcTask(block: Connection.() -> T): T =
        sessionFactory.fromStatelessSession {
            it.runJdbcTask {
                block()
            }
        }

    override fun <T> runTransactionalJdbcTask(block: context(TransactionContext) Connection.() -> T): T {
        return sessionFactory.fromStatelessSession {
            transactional((it as SharedSessionContractImplementor)) {
                it.runJdbcTask {
                    block(this@transactional, this@runJdbcTask)
                }
            }
        }
    }

    private data object TransactionContextMarker : TransactionContext

    private fun <T> transactional(session: SharedSessionContractImplementor, block: TransactionContext.() -> T): T {
        return if (session.transaction.isActive) {
            block(TransactionContextMarker)
        } else {
            val transactionObserver = object: TransactionObserver {

                override fun afterBegin() {
                    globalTransactionListeners.forEach {
                        it.afterBegin(session)
                    }
                }

                override fun beforeCompletion() {
                    globalTransactionListeners.forEach {
                        it.beforeCompletion(session)
                    }
                }

                override fun afterCompletion(successful: Boolean, delayed: Boolean) {
                    globalTransactionListeners.forEach {
                        it.afterCompletion(session, successful, delayed)
                    }
                }
            }

            session.transactionCoordinator.addObserver(transactionObserver)
            try {
                TransactionManagement.manageTransaction(
                    session,
                    session.beginTransaction(),
                    Function {
                        block(TransactionContextMarker)
                    }
                )
            } finally {
                session.transactionCoordinator.removeObserver(transactionObserver)

                globalTransactionListeners.forEach {
                    it.afterDispose(session)
                }
            }
        }
    }

    private fun <T> EntityManager.transactional(block: TransactionContext.() -> T): T =
        transactional(asHibernateSession.unwrap(SharedSessionContractImplementor::class.java), block)

    private fun <T> JpaSessionContext.transactional(block: TransactionContext.() -> T): T =
        entityManager.transactional { block(this) }
}
