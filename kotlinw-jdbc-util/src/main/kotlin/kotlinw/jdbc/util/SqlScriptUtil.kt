package kotlinw.jdbc.util

import java.sql.Connection
import java.sql.ResultSet

fun Connection.executeStatements(sqlScript: String, separator: String = ";\n") {
    createStatement().use { statement ->
        sqlScript.split(separator).forEach { statement.execute(it) }
    }
}

fun <T> Connection.executeSingleResultQuery(sql: String, resultConverter: ResultSet.() -> T): T? =
    createStatement().use {
        it.executeQuery(sql).use {
            if (it.next()) {
                it.resultConverter()
            } else {
                null
            }
        }
    }
