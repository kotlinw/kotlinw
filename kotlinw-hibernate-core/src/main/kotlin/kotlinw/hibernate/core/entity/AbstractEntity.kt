package kotlinw.hibernate.core.entity

import jakarta.persistence.MappedSuperclass
import java.io.Serializable

@MappedSuperclass
abstract class AbstractEntity<ID : Serializable> : Serializable {

    abstract val id: ID?

    abstract override fun equals(other: Any?): Boolean

    abstract override fun hashCode(): Int

    abstract override fun toString(): String
}
