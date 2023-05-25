package kotlinw.hibernate.core.schemaupgrade

import jakarta.persistence.EntityManager
import kotlinw.hibernate.core.api.TransactionContext
import java.sql.Connection

sealed interface DatabaseUpgrader

fun interface DatabaseStructureUpgrader : DatabaseUpgrader {

    context(Connection, TransactionContext)
    fun upgradeStructure()
}

fun interface DatabaseDataUpgrader : DatabaseUpgrader {

    context(EntityManager, TransactionContext)
    fun upgradeData()
}
