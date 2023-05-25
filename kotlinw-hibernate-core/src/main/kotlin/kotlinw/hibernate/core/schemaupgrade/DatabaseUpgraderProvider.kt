package kotlinw.hibernate.core.schemaupgrade

typealias SortableDatabaseUpgraderId = String

fun interface DatabaseUpgraderProvider {

    fun getUpgraders(): List<Pair<SortableDatabaseUpgraderId, DatabaseUpgrader>>
}
