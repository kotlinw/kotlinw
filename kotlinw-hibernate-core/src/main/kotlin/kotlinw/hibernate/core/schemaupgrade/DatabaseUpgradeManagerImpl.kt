package kotlinw.hibernate.core.schemaupgrade

import kotlinw.hibernate.core.api.jdbcTask
import kotlinw.hibernate.core.api.runReadOnlyJpaTask
import kotlinw.hibernate.core.api.runTransactionalJpaTask
import kotlinw.hibernate.core.entity.JpaSessionContext
import kotlinw.hibernate.core.entity.TransactionalJpaSessionContext
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.logging.platform.PlatformLogging
import org.hibernate.SessionFactory
import java.sql.Connection

class DatabaseUpgradeManagerImpl(
    private val sessionFactory: SessionFactory,
    private val databaseUpgraderProviders: Collection<DatabaseUpgraderProvider>,
    private val onStructureUpgraded: context(JpaSessionContext) (SortableDatabaseUpgraderId) -> Unit,
    private val onDataUpgraded: context(JpaSessionContext) (SortableDatabaseUpgraderId) -> Unit,
    private val findCurrentSchemaVersion: (Connection) -> SortableDatabaseUpgraderId?,
    private val checkDatabaseUpgraderAlreadyApplied: (context(JpaSessionContext) (SortableDatabaseUpgraderId) -> Boolean)?
) : DatabaseUpgradeManager {

    companion object {

        private val upgraderIdReqex = Regex("""\d{4}\d{2}\d{2}\d{2}\d{2}""")

        private const val emptySchemaVersion = "197001010000"
    }

    private val logger = PlatformLogging.getLogger()

    override fun upgradeSchema() {
        val currentSchemaVersion: String = sessionFactory.runReadOnlyJpaTask {
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
                            sessionFactory.runReadOnlyJpaTask {
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
                    sessionFactory.runTransactionalJpaTask {
                        applyUpgraders(outOfOrderUpgraders)
                    }
                }
            }

            applicationVersion < currentSchemaVersion -> {
                throw IllegalStateException("Obsolete application version: applicationVersion=$applicationVersion, currentSchemaVersion=$currentSchemaVersion")
            }

            else -> {
                logger.info { "Upgrading database schema version from " / currentSchemaVersion / " to " / applicationVersion }

                sessionFactory.runTransactionalJpaTask {
                    applyUpgraders(outOfOrderUpgraders)
                    applyUpgraders(normalUpgraders)
                }
            }
        }
    }

    context (TransactionalJpaSessionContext)
    private fun applyUpgraders(upgradersToApply: List<Pair<String, DatabaseUpgrader>>) {
        jdbcTask {
            upgradersToApply.forEach {
                val upgrader = it.second
                if (upgrader is DatabaseStructureUpgrader) {
                    logger.debug { "Upgrading structure to " / it.first }
                    try {
                        upgrader.upgradeStructure()
                        onStructureUpgraded(this@TransactionalJpaSessionContext, it.first)
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
                    with(this) {
                        upgrader.upgradeData()
                    }
                    onDataUpgraded(this@TransactionalJpaSessionContext, it.first)
                    entityManager.flush()
                } catch (e: Exception) {
                    throw RuntimeException("Data upgrade failed: ${it.first}", e)
                }
            }
        }
    }
}
