package kotlinw.hibernate.core.schemaupgrade

import java.sql.Connection
import kotlinw.hibernate.core.api.jdbcTask
import kotlinw.hibernate.core.service.JpaPersistenceService
import kotlinw.logging.api.LoggerFactory
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import xyz.kotlinw.jpa.api.JpaSessionContext
import xyz.kotlinw.jpa.api.TransactionContext

class DatabaseUpgradeManagerImpl(
    loggerFactory: LoggerFactory,
    private val jpaPersistenceService: JpaPersistenceService,
    private val databaseUpgraderProviders: Collection<DatabaseUpgraderProvider>,
    private val onStructureUpgraded: context(TransactionContext) JpaSessionContext.(SortableDatabaseUpgraderId) -> Unit,
    private val onDataUpgraded: context(TransactionContext) JpaSessionContext.(SortableDatabaseUpgraderId) -> Unit,
    private val findCurrentSchemaVersion: (Connection) -> SortableDatabaseUpgraderId?,
    private val checkDatabaseUpgraderAlreadyApplied: (context(JpaSessionContext) (SortableDatabaseUpgraderId) -> Boolean)?
) : DatabaseUpgradeManager {

    companion object {

        private val upgraderIdReqex = Regex("""\d{4}\d{2}\d{2}\d{2}\d{2}""")

        private const val emptySchemaVersion = "197001010000"
    }

    private val logger = loggerFactory.getLogger()

    override fun upgradeSchema() {
        val currentSchemaVersion: String = jpaPersistenceService.runJpaTask {
            jdbcTask { findCurrentSchemaVersion(this) } ?: emptySchemaVersion
        }

        val versionsToUpgraders =
            databaseUpgraderProviders.flatMap { it.getUpgraders() }.sortedBy { it.first }

        versionsToUpgraders.forEach {
            val upgraderId = it.first
            check(upgraderIdReqex.matches(upgraderId)) { "Invalid database upgrader ID: $upgraderId (invalid format)" }
            check(upgraderId > emptySchemaVersion) { "Invalid database upgrader ID: $upgraderId (must be >= $emptySchemaVersion)" }
        }

        val applicationVersion =
            if (versionsToUpgraders.isEmpty()) {
                currentSchemaVersion
            } else {
                versionsToUpgraders.last().first
            }

        val normalUpgraders = versionsToUpgraders.filter { it.first > currentSchemaVersion }

        val outOfOrderUpgraders =
            if (checkDatabaseUpgraderAlreadyApplied != null) {
                versionsToUpgraders.filter {
                    val schemaVersion = it.first
                    schemaVersion < currentSchemaVersion &&
                            jpaPersistenceService.runJpaTask {
                                !checkDatabaseUpgraderAlreadyApplied.invoke(
                                    this,
                                    schemaVersion
                                )
                            }

                }
            } else {
                logger.debug { "Out-of-order upgraders are ignored (if any exist)." }
                emptyList()
            }

        if (outOfOrderUpgraders.isNotEmpty()) {
            logger.info { "Out-of-order database schema upgraders: " / outOfOrderUpgraders }
        }

        when {
            applicationVersion == currentSchemaVersion -> {
                logger.info { "Database schema is up-to-date: " / currentSchemaVersion }

                if (outOfOrderUpgraders.isNotEmpty()) {
                    jpaPersistenceService.runTransactionalJpaTask {
                        applyUpgraders(outOfOrderUpgraders)
                    }
                }
            }

            applicationVersion < currentSchemaVersion -> {
                throw IllegalStateException("Obsolete application version: applicationVersion=$applicationVersion, currentSchemaVersion=$currentSchemaVersion")
            }

            else -> {
                logger.info { "Upgrading database schema version from " / currentSchemaVersion / " to " / applicationVersion }

                jpaPersistenceService.runTransactionalJpaTask {
                    applyUpgraders(outOfOrderUpgraders)
                    applyUpgraders(normalUpgraders)
                }
            }
        }
    }

    context (TransactionContext, JpaSessionContext)
    private fun applyUpgraders(upgradersToApply: List<Pair<String, DatabaseUpgrader>>) {
        jdbcTask {
            upgradersToApply.forEach {
                val upgrader = it.second
                if (upgrader is DatabaseStructureUpgrader) {
                    logger.debug { "Upgrading structure to " / it.first }
                    try {
                        upgrader.upgradeStructure()
                        with(this@TransactionContext) {
                            onStructureUpgraded(this@JpaSessionContext, it.first)
                        }
                    } catch (e: Exception) {
                        throw RuntimeException("Structure upgrade failed: ${it.first}", e)
                    }
                }
            }
        }

        upgradersToApply.forEach {
            val upgrader = it.second
            if (upgrader is DatabaseDataUpgrader) {
                logger.debug { "Upgrading data to " / it.first }
                try {
                    upgrader.upgradeData()
                    with(this@TransactionContext) {
                        onDataUpgraded(this@JpaSessionContext, it.first)
                    }
                    entityManager.flush()
                } catch (e: Exception) {
                    throw RuntimeException("Data upgrade failed: ${it.first}", e)
                }
            }
        }
    }
}
