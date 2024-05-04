package kotlinw.hibernate.core.api

import jakarta.persistence.EntityManager
import xyz.kotlinw.jpa.api.TypeSafeEntityManager

sealed interface JpaSessionContext {

    val entityManager: TypeSafeEntityManager
}

inline fun <T> JpaSessionContext.withEntityManager(block: context(TypeSafeEntityManager) () -> T): T =
    block(entityManager)

@JvmInline
internal value class JpaSessionContextImpl(override val entityManager: TypeSafeEntityManager) : JpaSessionContext
