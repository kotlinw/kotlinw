package xyz.kotlinw.jpa.repository

import jakarta.persistence.MappedSuperclass
import java.io.Serializable
import xyz.kotlinw.jpa.api.EntityReference

@MappedSuperclass
abstract class AbstractEntity<ID : Serializable> {

    abstract val id: ID?

    protected abstract val entityClass: Class<*>

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is AbstractEntity<*> || entityClass != other.entityClass) return false
        check(id != null) { "id must be generated or assigned before calling equals(): $this" }
        check(other.id != null) { "id must be generated or assigned before calling equals(): $other" }
        return id == other.id
    }

    override fun hashCode(): Int {
        check(id != null) { "id must be generated or assigned before calling hashCode()" }
        return id?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "${entityClass.simpleName}(id=$id)"
    }
}
