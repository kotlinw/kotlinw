package kotlinw.hibernate.core.api

import jakarta.persistence.EntityManager
import org.hibernate.SharedSessionContract
import org.hibernate.internal.TransactionManagement
import java.util.function.Function

fun <T> SharedSessionContract.transactional(block: context(Transactional) () -> T): T =
    if (isJoinedToTransaction) {
        block(TransactionalImpl)
    } else {
        manageTransaction {
            block(TransactionalImpl)
        }
    }

fun <T> EntityManager.transactional(block: context(Transactional) () -> T): T =
    (asHibernateSession as SharedSessionContract).transactional(block)

internal fun <T> SharedSessionContract.manageTransaction(block: () -> T): T =
    TransactionManagement.manageTransaction(
        this,
        beginTransaction(),
        Function { block() }
    )

fun <T> JpaSessionContext.transactional(block: context(Transactional) () -> T): T =
    entityManager.transactional {
        block(TransactionalImpl)
    }
