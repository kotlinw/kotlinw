package kotlinw.hibernate.core.api

import jakarta.persistence.EntityManager
import org.hibernate.SharedSessionContract
import java.sql.Connection
import xyz.kotlinw.jpa.api.JpaSessionContext

fun <T> SharedSessionContract.runJdbcTask(block: Connection.() -> T): T = doReturningWork(block)

fun <T> EntityManager.jdbcTask(block: Connection.() -> T): T = asHibernateSession.runJdbcTask(block)

fun <T> JpaSessionContext.jdbcTask(block: Connection.() -> T): T = entityManager.jdbcTask(block)
