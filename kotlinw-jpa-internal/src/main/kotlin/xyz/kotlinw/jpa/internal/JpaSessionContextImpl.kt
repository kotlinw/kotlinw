package xyz.kotlinw.jpa.internal

import xyz.kotlinw.jpa.api.JpaSessionContext
import xyz.kotlinw.jpa.api.TypedEntityManager

@JvmInline
value class JpaSessionContextImpl(override val entityManager: TypedEntityManager) : JpaSessionContext
