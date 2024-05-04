package kotlinw.hibernate.core.api

import org.hibernate.SessionFactory
import java.sql.Connection
import xyz.kotlinw.jpa.api.JpaSessionContext
import xyz.kotlinw.jpa.api.Transactional
import xyz.kotlinw.jpa.api.TransactionalImpl
import xyz.kotlinw.jpa.api.TypeSafeEntityManager
import xyz.kotlinw.jpa.core.asTypeSafeEntityManager
import xyz.kotlinw.jpa.internal.JpaSessionContextImpl

internal fun SessionFactory.createTypeSafeEntityManager(): TypeSafeEntityManager =
    createEntityManager().asTypeSafeEntityManager()

fun <T> SessionFactory.runJpaTask(block: JpaSessionContext.() -> T): T =
    createTypeSafeEntityManager().use {
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
        it.transactional {
            it.runJdbcTask {
                block()
            }
        }
    }
