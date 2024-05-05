package xyz.kotlinw.jpa.repository

import jakarta.persistence.MappedSuperclass
import java.io.Serializable
import java.util.*
import xyz.kotlinw.jpa.repository.spi.PersistenceProviderSupportProvider

@MappedSuperclass
abstract class AbstractEntity<ID : Serializable> {

    companion object {

        private val entityClassResolver: (Class<*>) -> Class<*>

        init {
            val providers = ServiceLoader.load(PersistenceProviderSupportProvider::class.java)
            // TODO WARN log if multiple providers present
            // TODO WARN log if no provider present and using default
            entityClassResolver = providers.firstOrNull()?.create()
                ?.let { support -> { support.resolveEntityClass(it) } } ?: { it }
        }
    }

    abstract val id: ID?

    protected fun resolveEntityClass(): Class<*> = entityClassResolver(this::class.java)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is AbstractEntity<*> || resolveEntityClass() != other.resolveEntityClass()) return false
        check(id != null) { "id must be generated or assigned before calling equals(): $this" }
        check(other.id != null) { "id must be generated or assigned before calling equals(): $other" }
        return id == other.id
    }

    override fun hashCode(): Int {
        check(id != null) { "id must be generated or assigned before calling hashCode()" }
        return id.hashCode()
    }

    override fun toString(): String {
        return "${resolveEntityClass().simpleName}(id=$id)"
    }
}
