package kotlinw.hibernate.core.entity

import kotlinw.hibernate.core.api.TypeSafeEntityManager

sealed interface JpaSessionContext {

    val entityManager: TypeSafeEntityManager
}

@JvmInline
internal value class JpaSessionContextImpl(override val entityManager: TypeSafeEntityManager) : JpaSessionContext

interface TransactionalJpaSessionContext : JpaSessionContext

@JvmInline
internal value class TransactionalJpaSessionContextImpl(override val entityManager: TypeSafeEntityManager) :
    TransactionalJpaSessionContext
