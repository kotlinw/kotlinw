package kotlinw.hibernate.core.schemaupgrade

import xyz.kotlinw.jpa.api.JpaSessionContext
import xyz.kotlinw.jpa.api.TransactionContext
import java.sql.Connection

sealed interface DatabaseUpgrader

fun interface DatabaseStructureUpgrader : DatabaseUpgrader {

    context(Connection)
    fun upgradeStructure()
}

fun interface DatabaseDataUpgrader : DatabaseUpgrader {

    context(TransactionContext, JpaSessionContext)
    fun upgradeData()
}
