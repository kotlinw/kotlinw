package kotlinw.hibernate.core.schemaupgrade

import jakarta.persistence.EntityManager
import kotlinw.hibernate.core.entity.JpaSessionContext
import kotlinw.hibernate.core.entity.TransactionalJpaSessionContext
import java.sql.Connection

sealed interface DatabaseUpgrader

fun interface DatabaseStructureUpgrader : DatabaseUpgrader {

    context(Connection)
    fun upgradeStructure()
}

fun interface DatabaseDataUpgrader : DatabaseUpgrader {

    context(TransactionalJpaSessionContext)
    fun upgradeData()
}
