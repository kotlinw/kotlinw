package xyz.kotlinw.jpa.api

interface JpaSessionContext {

    val entityManager: TypeSafeEntityManager
}

inline fun <T> JpaSessionContext.withEntityManager(block: TypeSafeEntityManager.() -> T): T =
    block(entityManager)
