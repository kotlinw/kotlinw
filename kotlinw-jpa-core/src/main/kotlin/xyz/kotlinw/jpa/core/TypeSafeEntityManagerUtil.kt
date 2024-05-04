package xyz.kotlinw.jpa.core

import jakarta.persistence.EntityManager
import xyz.kotlinw.jpa.api.TypeSafeEntityManager
import xyz.kotlinw.jpa.internal.TypeSafeEntityManagerImpl

fun EntityManager.asTypeSafeEntityManager(): TypeSafeEntityManager = TypeSafeEntityManagerImpl(this)
