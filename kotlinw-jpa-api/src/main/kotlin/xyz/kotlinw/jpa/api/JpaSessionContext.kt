package xyz.kotlinw.jpa.api

interface JpaSessionContext {

    val entityManager: TypedEntityManager
}

inline fun <T> JpaSessionContext.withEntityManager(block: TypedEntityManager.() -> T): T =
    block(entityManager)
