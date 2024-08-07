package kotlinw.module.hibernate

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinw.configuration.core.ConstantConfigurationPropertyResolver
import kotlinw.configuration.core.EnumerableConfigurationPropertyLookupSourceImpl
import kotlinw.hibernate.api.configuration.PersistentClassProvider
import kotlinw.hibernate.core.api.jdbcTask
import xyz.kotlinw.jpa.api.JpaSessionContext
import kotlinw.hibernate.core.schemaexport.ExportedSchemaScriptType
import kotlinw.hibernate.core.service.JpaPersistenceService
import kotlinw.jdbc.util.executeStatements
import kotlinw.module.hibernate.core.HibernateModule
import kotlinw.module.hibernate.tool.HibernateSqlSchemaExporterModule
import kotlinw.module.hibernate.tool.HibernateSqlSchemaExporterScope
import kotlinx.coroutines.test.runTest
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.ComponentQuery
import xyz.kotlinw.di.api.Container
import xyz.kotlinw.di.api.Module
import xyz.kotlinw.di.api.Scope
import xyz.kotlinw.di.api.close
import xyz.kotlinw.di.api.start
import xyz.kotlinw.jpa.core.createTypedQuery

@Entity
@Table(name = "Person")
class PersonEntity(

    @Id
    var id: Long,

    var name: String
)

@Container
interface TestContainer {

    companion object

    @Module(includeModules = [HibernateModule::class, HibernateSqlSchemaExporterModule::class])
    class TestModule {

        @Component
        fun persistentClassProvider() = PersistentClassProvider { listOf(PersonEntity::class) }

        @Component
        fun configurationLookupSource() =
            EnumerableConfigurationPropertyLookupSourceImpl(
                ConstantConfigurationPropertyResolver.of("hibernate.connection.url" to "jdbc:h2:mem:")
            )
    }

    interface TestScope : HibernateSqlSchemaExporterScope {

        @ComponentQuery
        fun jpaPersistenceService(): JpaPersistenceService
    }

    @Scope(modules = [TestModule::class])
    fun testScope(): TestScope
}

class HibernateModuleIntegrationTest {

    @Test
    fun testBootstrap() = runTest {
        TestContainer.create().testScope().apply {
            start()
            try {
                val sqlSchemaExporter = hibernateSqlSchemaExporter()

                val createSchemaScript = sqlSchemaExporter.exportSchema(ExportedSchemaScriptType.Create)
                assertEquals(
                    """
                    create table Person (id bigint not null, name varchar(255), primary key (id));
                    """.trimIndent(),
                    createSchemaScript
                )

                val jpaPersistenceService = jpaPersistenceService()

                jpaPersistenceService.runTransactionalJpaTask {
                    jdbcTask {
                        executeStatements(createSchemaScript)
                    }
                }

                fun JpaSessionContext.findAllPersons() =
                    entityManager.createTypedQuery("FROM PersonEntity", PersonEntity::class.java).resultList

                jpaPersistenceService.runJpaTask {
                    assertEquals(emptyList(), findAllPersons())
                }

                jpaPersistenceService.runTransactionalJpaTask {
                    entityManager.persistEntity(PersonEntity(1, "Joe"))
                    assertEquals(1, findAllPersons().size)
                }

                jpaPersistenceService.runJpaTask {
                    assertEquals(1, findAllPersons().size)
                }
            } finally {
                close()
            }
        }
    }
}
