package xyz.kotlinw.jpa.repository.spi

interface PersistenceProviderSupport {

    fun resolveEntityClass(runtimeClass: Class<*>): Class<*>
}

interface PersistenceProviderSupportProvider {

    fun create(): PersistenceProviderSupport
}
