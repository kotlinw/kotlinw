package kotlinw.hibernate.core.api

import jakarta.persistence.EntityManager
import kotlinw.util.stdlib.DelicateKotlinwApi
import org.hibernate.SessionFactory
import org.hibernate.SharedSessionContract
import org.hibernate.Transaction
import org.hibernate.internal.TransactionManagement.manageTransaction
import java.sql.Connection
import java.util.function.Function

@DelicateKotlinwApi
fun EntityManager.asTypeSafeEntityManager(): TypeSafeEntityManager = TypeSafeEntityManagerImpl(this)

@OptIn(DelicateKotlinwApi::class)
internal fun SessionFactory.createTypeSafeEntityManager(): TypeSafeEntityManager =
    createEntityManager().asTypeSafeEntityManager()

// TODO ez nem feltétlenül read-only, csak nem tranzakcionális, ezt a nevének is tükröznie kellene
fun <T> SessionFactory.runReadOnlyJpaTask(block: JpaSessionContext.() -> T): T =
    createTypeSafeEntityManager().use {
        block(JpaSessionContextImpl(it))
    }

context(JpaSessionContext)
@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated(message = "Use runInNewTransaction() instead.", level = DeprecationLevel.ERROR)
fun <T> SessionFactory.runTransactionalJpaTask(block: JpaSessionContext.() -> T): T = throw IllegalStateException()

fun <T> SessionFactory.runTransactionalJpaTask(block: TransactionalJpaSessionContext.() -> T): T =
    createTypeSafeEntityManager().use {
        it.asHibernateSession.transactional {
            block(TransactionalJpaSessionContextImpl(it))
        }
    }

fun <T> SessionFactory.runJdbcTask(isTransactional: Boolean = true, block: Connection.() -> T): T =
    fromStatelessSession {
        if (isTransactional) {
            it.transactional {
                runJdbcTask {
                    block()
                }
            }
        } else {
            runJdbcTask {
                block()
            }
        }
    }

internal class TransactionalContextImpl(val transaction: Transaction) : TransactionalContext

internal fun <T, S : SharedSessionContract> S.transactional(block: context(TransactionalContext) S.() -> T): T {
    val transaction = beginTransaction()
    return manageTransaction(
        this,
        transaction,
        Function {
            with(TransactionalContextImpl(transaction)) {
                block(it)
            }
        }
    )
}
