package kotlinw.hibernate.core.schemaupgrade.simple

import jakarta.persistence.EntityManager
import kotlinw.hibernate.core.schemaupgrade.DatabaseUpgradeManager
import kotlinw.hibernate.core.schemaupgrade.DatabaseUpgradeManagerImpl
import kotlinw.hibernate.core.schemaupgrade.DatabaseUpgraderProvider
import kotlinw.jdbc.util.executeSingleResultQuery
import org.hibernate.SessionFactory
import java.time.Instant

fun SimpleDatabaseUpgradeManager(
    sessionFactory: SessionFactory,
    databaseUpgraderProviders: Collection<DatabaseUpgraderProvider>,
    checkSchemaVersionTableExistsQueryString: String = "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE LOWER(table_name)=LOWER('DatabaseSchemaVersionInfo'))",
    selectSchemaVersionQueryString: String = "SELECT currentSchemaVersion FROM DatabaseSchemaVersionInfo"
): DatabaseUpgradeManager {

    fun updateSchemaVersionInfo(entityManager: EntityManager, schemaVersion: String) {
        entityManager.createQuery("FROM DatabaseSchemaVersionInfoEntity", DatabaseSchemaVersionInfoEntity::class.java)
            .singleResult
            .also {
                it.currentSchemaVersion = schemaVersion
                it.timestamp = Instant.now()
            }
    }

    return DatabaseUpgradeManagerImpl(
        sessionFactory,
        databaseUpgraderProviders,
        ::updateSchemaVersionInfo,
        ::updateSchemaVersionInfo,
        {
            val tableExists =
                it.executeSingleResultQuery(checkSchemaVersionTableExistsQueryString) { getBoolean(1) } ?: false
            if (tableExists) {
                it.executeSingleResultQuery(selectSchemaVersionQueryString) { getString(1) }
            } else {
                null
            }
        },
        null
    )
}
