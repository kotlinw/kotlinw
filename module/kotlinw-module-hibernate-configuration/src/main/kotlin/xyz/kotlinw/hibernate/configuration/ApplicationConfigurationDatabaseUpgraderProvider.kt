package xyz.kotlinw.hibernate.configuration

import kotlinw.hibernate.core.schemaupgrade.DatabaseStructureUpgrader
import kotlinw.hibernate.core.schemaupgrade.DatabaseUpgrader
import kotlinw.hibernate.core.schemaupgrade.DatabaseUpgraderProvider
import kotlinw.hibernate.core.schemaupgrade.SortableDatabaseUpgraderId
import kotlinw.jdbc.util.executeStatements
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.hibernate.configuration.entity.ApplicationConfigurationEntity

@Component
class ApplicationConfigurationDatabaseUpgraderProvider : DatabaseUpgraderProvider {

    override fun getUpgraders() = listOf<Pair<SortableDatabaseUpgraderId, DatabaseUpgrader>>(
        "202401102029" to DatabaseStructureUpgrader {
            executeStatements(
                """
create table ApplicationConfiguration (id bigint not null, uid uuid not null, name TEXT not null, value TEXT not null, primary key (id));
create table ApplicationConfiguration_AUD (id bigint not null, REV integer not null, REVTYPE smallint, name TEXT, value TEXT, primary key (REV, id));
alter table if exists ApplicationConfiguration drop constraint if exists UK_tbrnwh128gc3c7iwdpsbbtplm;
alter table if exists ApplicationConfiguration add constraint UK_tbrnwh128gc3c7iwdpsbbtplm unique (uid);
alter table if exists ApplicationConfiguration_AUD add constraint FKh3clamgp9vl6lg6tv30es8s68 foreign key (REV) references REVINFO;
                """.trimIndent()
            )
        }
    )
}
