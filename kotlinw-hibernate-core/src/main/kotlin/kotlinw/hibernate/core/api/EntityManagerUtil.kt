package kotlinw.hibernate.core.api

import jakarta.persistence.EntityManager
import org.hibernate.Session
import java.sql.Connection

interface TransactionContext

private object TransactionContextImpl : TransactionContext

fun <T, E : EntityManager> E.transactional(block: context(TransactionContext) E.() -> T): T =
    if (transaction.isActive) {
        block(TransactionContextImpl, this)
    } else {
        transaction.begin()
        try {
            val result = block(TransactionContextImpl, this)
            transaction.commit()
            result
        } catch (e: Exception) {
            transaction.rollback()
            throw e
        }
    }

val EntityManager.hibernateSession: Session get() = unwrap(Session::class.java)

fun <T> EntityManager.jdbcTask(block: Connection.() -> T): T = hibernateSession.doReturningWork(block)

