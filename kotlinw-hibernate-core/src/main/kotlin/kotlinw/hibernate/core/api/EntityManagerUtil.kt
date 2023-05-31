package kotlinw.hibernate.core.api

import jakarta.persistence.EntityManager
import kotlinw.hibernate.core.entity.JpaSessionContext
import org.hibernate.Session
import java.sql.Connection

val EntityManager.asHibernateSession: Session get() = unwrap(Session::class.java)

fun <T> EntityManager.jdbcTask(block: Connection.() -> T): T = asHibernateSession.doReturningWork(block)

fun <T> JpaSessionContext.jdbcTask(block: Connection.() -> T): T = entityManager.jdbcTask(block)
