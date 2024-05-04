package xyz.kotlinw.jpa.internal

import xyz.kotlinw.jpa.api.JpaSessionContext
import xyz.kotlinw.jpa.api.TypeSafeEntityManager

@JvmInline
value class JpaSessionContextImpl(override val entityManager: TypeSafeEntityManager) : JpaSessionContext
