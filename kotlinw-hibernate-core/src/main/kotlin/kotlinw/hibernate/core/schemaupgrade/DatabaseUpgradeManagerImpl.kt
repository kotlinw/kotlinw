package kotlinw.hibernate.core.schemaupgrade

import jakarta.persistence.EntityManager
import kotlinw.hibernate.core.api.TransactionContext
import kotlinw.hibernate.core.api.TypeSafeEntityManager
import kotlinw.hibernate.core.api.createTypeSafeEntityManager
import kotlinw.hibernate.core.api.jdbcTask
import kotlinw.hibernate.core.api.transactional
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.logging.platform.PlatformLogging
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.hibernate.SessionFactory
import java.sql.Connection

class DatabaseUpgradeManagerImpl(
    private val sessionFactory: SessionFactory,
    private val databaseUpgraderProviders: Collection<DatabaseUpgraderProvider>,
    private val onStructureUpgraded: (EntityManager, SortableDatabaseUpgraderId) -> Unit,
    private val onDataUpgraded: (EntityManager, SortableDatabaseUpgraderId) -> Unit,
    private val findCurrentSchemaVersion: (Connection) -> SortableDatabaseUpgraderId?,
    private val checkDatabaseUpgraderAlreadyApplied: ((EntityManager, SortableDatabaseUpgraderId) -> Boolean)?
) : DatabaseUpgradeManager {

    companion object {

        private val upgraderIdReqex = Regex("""\d{4}\d{2}\d{2}\d{2}\d{2}""")

        private const val emptySchemaVersion = "197001010000"
    }

    private val logger = PlatformLogging.getLogger()

    override fun performInitialization() {
        upgradeSchema(sessionFactory.createTypeSafeEntityManager())
    }

    private fun upgradeSchema(entityManager: TypeSafeEntityManager) {
        val currentSchemaVersion: String =
            entityManager.jdbcTask { findCurrentSchemaVersion(this) } ?: emptySchemaVersion

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
                            !checkDatabaseUpgraderAlreadyApplied.invoke(entityManager, schemaVersion)
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
                    entityManager.transactional {
                        applyUpgraders(outOfOrderUpgraders)
                    }
                }
            }

            applicationVersion < currentSchemaVersion -> {
                throw IllegalStateException("Obsolete application version: applicationVersion=$applicationVersion, currentSchemaVersion=$currentSchemaVersion")
            }

            else -> {
                logger.info { "Upgrading database schema version from " / currentSchemaVersion / " to " / applicationVersion }

                entityManager.transactional {
                    applyUpgraders(outOfOrderUpgraders)
                    applyUpgraders(normalUpgraders)
                }
            }
        }
    }

    context (TransactionContext)
    private fun EntityManager.applyUpgraders(upgradersToApply: List<Pair<String, DatabaseUpgrader>>) {
        jdbcTask {
            upgradersToApply.forEach {
                val upgrader = it.second
                if (upgrader is DatabaseStructureUpgrader) {
                    logger.debug { "Upgrading structure to " / it.first }
                    try {
                        upgrader.upgradeStructure()
                        onStructureUpgraded(this@applyUpgraders, it.first)
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
                    onDataUpgraded(this, it.first)
                    flush()
                } catch (e: Exception) {
                    throw RuntimeException("Data upgrade failed: ${it.first}", e)
                }
            }
        }
    }
}
