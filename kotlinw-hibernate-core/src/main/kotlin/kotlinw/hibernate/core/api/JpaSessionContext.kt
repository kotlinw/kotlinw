package kotlinw.hibernate.core.api

import org.hibernate.Transaction

sealed interface JpaSessionContext {

    val entityManager: TypeSafeEntityManager
}

@JvmInline
internal value class JpaSessionContextImpl(override val entityManager: TypeSafeEntityManager) : JpaSessionContext

sealed interface TransactionalJpaSessionContext : JpaSessionContext, TransactionalContext

@JvmInline
internal value class TransactionalJpaSessionContextImpl(override val entityManager: TypeSafeEntityManager) :
    TransactionalJpaSessionContext {

    override val transaction get() = entityManager.transaction as Transaction
}
