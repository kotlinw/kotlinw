package kotlinw.hibernate.core.api

import org.hibernate.SessionFactory

fun SessionFactory.createTypeSafeEntityManager(): TypeSafeEntityManager =
    TypeSafeEntityManagerImpl(createEntityManager())
