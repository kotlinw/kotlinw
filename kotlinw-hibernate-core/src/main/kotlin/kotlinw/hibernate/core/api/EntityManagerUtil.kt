package kotlinw.hibernate.core.api

import jakarta.persistence.EntityManager
import org.hibernate.Session
import org.hibernate.SharedSessionContract
import org.hibernate.internal.TransactionManagement
import java.util.function.Function

val EntityManager.asHibernateSession: Session get() = unwrap(Session::class.java)
