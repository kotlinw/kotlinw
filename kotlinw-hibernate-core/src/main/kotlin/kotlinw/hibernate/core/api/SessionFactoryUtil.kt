package kotlinw.hibernate.core.api

import jakarta.persistence.EntityManager
import org.hibernate.SessionFactory
import java.sql.Connection

fun EntityManager.asTypeSafeEntityManager(): TypeSafeEntityManager = TypeSafeEntityManagerImpl(this)

internal fun SessionFactory.createTypeSafeEntityManager(): TypeSafeEntityManager =
    createEntityManager().asTypeSafeEntityManager()

fun <T> SessionFactory.runJpaTask(block: JpaSessionContext.() -> T): T =
    createTypeSafeEntityManager().use {
        block(JpaSessionContextImpl(it))
    }

fun <T> SessionFactory.runTransactionalJpaTask(block: TransactionalJpaSessionContext.() -> T): T =
    runJpaTask {
        transactional {
            block(TransactionalJpaSessionContextImpl(entityManager))
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
