package kotlinw.hibernate.core.api

import org.hibernate.SessionFactory

fun SessionFactory.createTypeSafeEntityManager(): TypeSafeEntityManager =
    TypeSafeEntityManagerImpl(createEntityManager())

fun <T> SessionFactory.jpaTask(block: TypeSafeEntityManager.() -> T): T =
    createTypeSafeEntityManager().use(block)
