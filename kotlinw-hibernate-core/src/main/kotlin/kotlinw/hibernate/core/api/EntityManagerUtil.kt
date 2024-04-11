package kotlinw.hibernate.core.api

import jakarta.persistence.EntityManager
import org.hibernate.Session

val EntityManager.asHibernateSession: Session get() = unwrap(Session::class.java)
