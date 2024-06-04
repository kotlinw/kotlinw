package kotlinw.hibernate.core.service

import java.sql.Connection
import kotlinw.hibernate.core.api.runJdbcTask
import kotlinw.hibernate.core.api.transactional
import org.hibernate.SessionFactory
import org.hibernate.engine.spi.SharedSessionContractImplementor
import xyz.kotlinw.jpa.api.JpaSessionContext
import xyz.kotlinw.jpa.api.Transactional
import xyz.kotlinw.jpa.api.TransactionalImpl
import xyz.kotlinw.jpa.api.TypedEntityManager
import xyz.kotlinw.jpa.core.asTypedEntityManager
import xyz.kotlinw.jpa.internal.JpaSessionContextImpl

interface JpaPersistenceService {

    val sessionFactory: SessionFactory // TODO ehelyett inkább specifikus metódusok kellenének, pl. listener-ek regisztrálására

    fun <T> runJpaTask(block: JpaSessionContext.() -> T): T

    fun <T> runTransactionalJpaTask(block: context(Transactional) JpaSessionContext.() -> T): T

    fun <T> runJdbcTask(block: Connection.() -> T): T

    fun <T> runTransactionalJdbcTask(block: context(Transactional)  Connection.() -> T): T
}

class JpaPersistenceServiceImpl(
    override val sessionFactory: SessionFactory
) : JpaPersistenceService {

    private fun createTypedEntityManager(): TypedEntityManager =
        sessionFactory.createEntityManager().asTypedEntityManager()

    override fun <T> runJpaTask(block: JpaSessionContext.() -> T): T =
        createTypedEntityManager().use {
            block(JpaSessionContextImpl(it))
        }

    override fun <T> runTransactionalJpaTask(block: context(Transactional) JpaSessionContext.() -> T): T =
        runJpaTask {
            transactional {
                block(TransactionalImpl, this@runJpaTask)
            }
        }

    override fun <T> runJdbcTask(block: Connection.() -> T): T =
        sessionFactory.fromStatelessSession {
            it.runJdbcTask {
                block()
            }
        }

    override fun <T> runTransactionalJdbcTask(block: context(Transactional) Connection.() -> T): T =
        sessionFactory.fromStatelessSession {
            (it as SharedSessionContractImplementor).transactional {
                it.runJdbcTask {
                    block(TransactionalImpl, this)
                }
            }
        }
}
