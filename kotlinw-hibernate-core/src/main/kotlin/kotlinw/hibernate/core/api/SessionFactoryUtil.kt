package kotlinw.hibernate.core.api

import kotlinw.hibernate.core.entity.JpaSessionContext
import kotlinw.hibernate.core.entity.JpaSessionContextImpl
import kotlinw.hibernate.core.entity.TransactionalJpaSessionContext
import kotlinw.hibernate.core.entity.TransactionalJpaSessionContextImpl
import kotlinw.util.stdlib.DelicateKotlinwApi
import org.hibernate.SessionFactory

internal fun SessionFactory.createTypeSafeEntityManager(): TypeSafeEntityManager =
    TypeSafeEntityManagerImpl(createEntityManager())

context(JpaSessionContext)
@DelicateKotlinwApi
fun <T> SessionFactory.runReadOnlyJpaTask(block: JpaSessionContext.() -> T): T = block(this@JpaSessionContext)

fun <T> SessionFactory.runReadOnlyJpaTask(block: JpaSessionContext.() -> T): T =
    createTypeSafeEntityManager().use {
        block(JpaSessionContextImpl(it))
    }

context(JpaSessionContext)
@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated(message = "Use runInNewTransaction() instead.", level = DeprecationLevel.ERROR)
fun <T> SessionFactory.runTransactionalJpaTask(block: JpaSessionContext.() -> T): T = throw IllegalStateException()

fun <T> SessionFactory.runTransactionalJpaTask(block: TransactionalJpaSessionContext.() -> T): T =
    createTypeSafeEntityManager().use { entityManager ->
        check(!entityManager.transaction.isActive)

        entityManager.transaction.begin()
        try {
            val result = block(TransactionalJpaSessionContextImpl(entityManager))
            entityManager.transaction.commit()
            result
        } catch (e: Exception) {
            entityManager.transaction.rollback()
            throw e
        }
    }
