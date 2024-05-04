package kotlinw.hibernate.core.entity

import java.io.Serializable
import org.hibernate.Hibernate
import xyz.kotlinw.jpa.repository.AbstractEntity

abstract class AbstractHibernateEntity<ID : Serializable> : AbstractEntity<ID>() {

    final override val entityClass: Class<*> get() = Hibernate.getClassLazy(this)
}
