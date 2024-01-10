package kotlinw.util.stdlib

import java.sql.Connection
import java.sql.DriverManager
import java.util.*

fun Connection.executeStatements(sqlScript: String, separator: String = ";\n") {
    createStatement().use { statement ->
        sqlScript.split(separator).forEach { statement.execute(it) }
    }
}

@DelicateKotlinwApi
fun <T> executeDirectPostgresqlOperation(
    connectionUrl: String,
    connectionUsername: String,
    connectionPassword: String,
    block: Connection.() -> T
): T =
    DriverManager.getConnection(connectionUrl,
        Properties().apply {
            setProperty("user", connectionUsername)
            setProperty("password", connectionPassword)
        }
    ).use {
        block(it!!)
    }
