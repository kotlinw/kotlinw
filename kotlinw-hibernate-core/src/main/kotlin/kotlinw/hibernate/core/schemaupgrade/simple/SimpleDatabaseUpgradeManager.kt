package kotlinw.hibernate.core.schemaupgrade.simple

import kotlinw.hibernate.core.api.JpaSessionContext
import xyz.kotlinw.jpa.api.Transactional
import kotlinw.hibernate.core.schemaupgrade.DatabaseUpgradeManager
import kotlinw.hibernate.core.schemaupgrade.DatabaseUpgradeManagerImpl
import kotlinw.hibernate.core.schemaupgrade.DatabaseUpgraderProvider
import kotlinw.hibernate.core.schemaupgrade.SortableDatabaseUpgraderId
import kotlinw.jdbc.util.executeSingleResultQuery
import kotlinw.logging.api.LoggerFactory
import org.hibernate.SessionFactory
import java.time.Instant

context(Transactional, JpaSessionContext)
private fun updateSchemaVersionInfoEntity(schemaVersion: SortableDatabaseUpgraderId) {
    entityManager.createQuery("FROM DatabaseSchemaVersionInfoEntity", DatabaseSchemaVersionInfoEntity::class.java)
        .resultList
        .also {
            if (it.isEmpty()) {
                entityManager.persistEntity(
                    DatabaseSchemaVersionInfoEntity(schemaVersion, Instant.now())
                )
            } else {
                it.first().apply {
                    currentSchemaVersion = schemaVersion
                    timestamp = Instant.now()
                }
            }
        }
}

fun SimpleDatabaseUpgradeManager(
    loggerFactory: LoggerFactory,
    sessionFactory: SessionFactory,
    databaseUpgraderProviders: Collection<DatabaseUpgraderProvider>,
    checkSchemaVersionTableExistsQueryString: String = "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE LOWER(table_name)=LOWER('DatabaseSchemaVersionInfo'))",
    selectSchemaVersionQueryString: String = "SELECT currentSchemaVersion FROM DatabaseSchemaVersionInfo"
): DatabaseUpgradeManager {

    return DatabaseUpgradeManagerImpl(
        loggerFactory,
        sessionFactory,
        databaseUpgraderProviders,
        ::updateSchemaVersionInfoEntity,
        ::updateSchemaVersionInfoEntity,
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
