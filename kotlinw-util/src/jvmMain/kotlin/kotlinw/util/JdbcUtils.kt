package kotlinw.util

import java.sql.Connection

fun Connection.executeStatements(sqlScript: String, separator: String = ";\n") {
    createStatement().use { statement ->
        sqlScript.split(separator).forEach { statement.execute(it) }
    }
}
