package xyz.kotlinw.hibernate.repository.spi

import org.hibernate.Hibernate
import xyz.kotlinw.jpa.repository.spi.PersistenceProviderSupport
import xyz.kotlinw.jpa.repository.spi.PersistenceProviderSupportProvider

class HibernatePersistenceProviderSupport: PersistenceProviderSupport {

    override fun resolveEntityClass(runtimeClass: Class<*>): Class<*> =
        Hibernate.getClassLazy(runtimeClass)
}

class HibernatePersistenceProviderSupportProvider: PersistenceProviderSupportProvider {

    override fun create(): PersistenceProviderSupport = HibernatePersistenceProviderSupport()
}
