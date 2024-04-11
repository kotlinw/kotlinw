package kotlinw.hibernate.core.api

sealed interface JpaSessionContext {

    val entityManager: TypeSafeEntityManager
}

@JvmInline
internal value class JpaSessionContextImpl(override val entityManager: TypeSafeEntityManager) : JpaSessionContext

sealed interface TransactionalJpaSessionContext: JpaSessionContext, Transactional

@JvmInline
internal value class TransactionalJpaSessionContextImpl(override val entityManager: TypeSafeEntityManager) : TransactionalJpaSessionContext
