package kotlinw.hibernate.core.api

import java.sql.Connection
import org.hibernate.SessionFactory
import org.hibernate.engine.spi.SharedSessionContractImplementor
import xyz.kotlinw.jpa.api.JpaSessionContext
import xyz.kotlinw.jpa.api.Transactional
import xyz.kotlinw.jpa.api.TransactionalImpl
import xyz.kotlinw.jpa.api.TypedEntityManager
import xyz.kotlinw.jpa.core.asTypedEntityManager
import xyz.kotlinw.jpa.internal.JpaSessionContextImpl

internal fun SessionFactory.createTypedEntityManager(): TypedEntityManager =
    createEntityManager().asTypedEntityManager()

fun <T> SessionFactory.runJpaTask(block: JpaSessionContext.() -> T): T =
    createTypedEntityManager().use {
        block(JpaSessionContextImpl(it))
    }

fun <T> SessionFactory.runTransactionalJpaTask(block: context(Transactional) JpaSessionContext.() -> T): T =
    runJpaTask {
        transactional {
            block(TransactionalImpl, this@runJpaTask)
        }
    }

fun <T> SessionFactory.runJdbcTask(block: Connection.() -> T): T =
    fromStatelessSession {
        it.runJdbcTask {
            block()
        }
    }

fun <T> SessionFactory.runTransactionalJdbcTask(block: Connection.() -> T): T =
    fromStatelessSession {
        (it as SharedSessionContractImplementor).transactional {
            it.runJdbcTask {
                block()
            }
        }
    }
