package kotlinw.hibernate.core.api

sealed interface JpaSessionContext {

    val entityManager: TypeSafeEntityManager
}

@JvmInline
internal value class JpaSessionContextImpl(override val entityManager: TypeSafeEntityManager) : JpaSessionContext

interface TransactionalJpaSessionContext : JpaSessionContext, TransactionalContext

@JvmInline
internal value class TransactionalJpaSessionContextImpl(override val entityManager: TypeSafeEntityManager) :
    TransactionalJpaSessionContext
