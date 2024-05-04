package xyz.kotlinw.jpa.core

import jakarta.persistence.EntityManager
import xyz.kotlinw.jpa.api.TypedEntityManager
import xyz.kotlinw.jpa.internal.TypedEntityManagerImpl

fun EntityManager.asTypedEntityManager(): TypedEntityManager = TypedEntityManagerImpl(this)
