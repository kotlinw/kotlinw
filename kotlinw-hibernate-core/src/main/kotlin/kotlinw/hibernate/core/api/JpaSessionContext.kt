package kotlinw.hibernate.core.api

sealed interface JpaSessionContext {

    val entityManager: TypeSafeEntityManager
}

@JvmInline
internal value class JpaSessionContextImpl(override val entityManager: TypeSafeEntityManager) : JpaSessionContext
