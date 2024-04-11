package kotlinw.hibernate.core.api

import org.hibernate.Transaction

sealed interface TransactionalContext {

    val transaction: Transaction
}
