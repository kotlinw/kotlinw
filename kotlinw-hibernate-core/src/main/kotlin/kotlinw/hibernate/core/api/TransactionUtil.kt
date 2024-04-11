package kotlinw.hibernate.core.api

import jakarta.persistence.EntityManager
import org.hibernate.SharedSessionContract
import org.hibernate.internal.TransactionManagement
import java.util.function.Function

fun <T> SharedSessionContract.transactional(block: () -> T): T =
    if (isJoinedToTransaction) {
        block()
    } else {
        manageTransaction {
            block()
        }
    }

fun <T> EntityManager.transactional(block: () -> T): T =
    (asHibernateSession as SharedSessionContract).transactional(block)

internal fun <T> SharedSessionContract.manageTransaction(block: () -> T): T =
    TransactionManagement.manageTransaction(
        this,
        beginTransaction(),
        Function { block() }
    )

fun <T> JpaSessionContext.transactional(block: () -> T): T =
    entityManager.transactional {
        block()
    }
