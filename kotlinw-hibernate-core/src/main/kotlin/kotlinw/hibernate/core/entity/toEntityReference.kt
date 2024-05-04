package kotlinw.hibernate.core.entity

import java.io.Serializable
import org.hibernate.Hibernate
import xyz.kotlinw.jpa.api.EntityReference
import xyz.kotlinw.jpa.repository.AbstractEntity

inline fun <reified E : AbstractEntity<ID>, ID : Serializable> E.toEntityReference() =
    EntityReference(Hibernate.getClass(this).kotlin, id ?: throw IllegalStateException())  // TODO exc
