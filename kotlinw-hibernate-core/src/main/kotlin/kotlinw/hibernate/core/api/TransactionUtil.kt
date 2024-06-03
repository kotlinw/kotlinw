package kotlinw.hibernate.core.api

import jakarta.persistence.EntityManager
import java.util.function.Function
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.internal.TransactionManagement
import org.hibernate.resource.transaction.spi.TransactionObserver
import xyz.kotlinw.jpa.api.JpaSessionContext
import xyz.kotlinw.jpa.api.Transactional
import xyz.kotlinw.jpa.api.TransactionalImpl

// TODO integration test, hogy tényleg jól működik önmagába "beágyazva"
internal fun <T> SharedSessionContractImplementor.transactional(block: context(Transactional) () -> T): T =
    if (isJoinedToTransaction) {
        block(TransactionalImpl)
    } else {
        manageTransaction {
            block(TransactionalImpl)
        }
    }

private class TransactionObserverImpl(private val session: SharedSessionContractImplementor) : TransactionObserver {

    override fun afterBegin() {
        println(">> afterBegin") // TODO reactive
    }

    override fun beforeCompletion() {
    }

    override fun afterCompletion(successful: Boolean, delayed: Boolean) {
        if (successful) {
            // TODO reactive
        }
    }
}

internal fun <T> SharedSessionContractImplementor.manageTransaction(block: () -> T): T {
    val observer = TransactionObserverImpl(this)
    transactionCoordinator.addObserver(observer)
    return try {
        TransactionManagement.manageTransaction(
            this,
            beginTransaction(),
            Function { block() }
        )
    } finally {
        transactionCoordinator.removeObserver(observer)
    }
}

fun <T> EntityManager.transactional(block: context(Transactional) () -> T): T =
    asHibernateSession.unwrap(SharedSessionContractImplementor::class.java).transactional(block)

fun <T> JpaSessionContext.transactional(block: context(Transactional) () -> T): T =
    entityManager.transactional {
        block(TransactionalImpl)
    }
