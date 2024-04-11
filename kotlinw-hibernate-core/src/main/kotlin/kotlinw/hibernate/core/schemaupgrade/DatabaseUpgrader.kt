package kotlinw.hibernate.core.schemaupgrade

import kotlinw.hibernate.core.api.JpaSessionContext
import kotlinw.hibernate.core.api.Transactional
import java.sql.Connection

sealed interface DatabaseUpgrader

fun interface DatabaseStructureUpgrader : DatabaseUpgrader {

    context(Connection)
    fun upgradeStructure()
}

fun interface DatabaseDataUpgrader : DatabaseUpgrader {

    context(Transactional, JpaSessionContext)
    fun upgradeData()
}
