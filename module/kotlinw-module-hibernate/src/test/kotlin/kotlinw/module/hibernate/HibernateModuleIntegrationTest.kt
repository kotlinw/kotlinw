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
import kotlinw.hibernate.core.api.runNonTransactionalJpaTask
import kotlinw.hibernate.core.api.runTransactionalJpaTask
import kotlinw.hibernate.core.api.JpaSessionContext
import kotlinw.hibernate.core.schemaexport.ExportedSchemaScriptType
import kotlinw.jdbc.util.executeStatements
import kotlinw.module.hibernate.core.HibernateModule
import kotlinw.module.hibernate.tool.HibernateSqlSchemaExporterModule
import kotlinw.module.hibernate.tool.HibernateSqlSchemaExporterScope
import kotlinx.coroutines.test.runTest
import org.hibernate.SessionFactory
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.ComponentQuery
import xyz.kotlinw.di.api.Container
import xyz.kotlinw.di.api.Module
import xyz.kotlinw.di.api.Scope

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
        fun sessionFactory(): SessionFactory
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

                val sessionFactory = sessionFactory()

                sessionFactory.runTransactionalJpaTask {
                    jdbcTask {
                        executeStatements(createSchemaScript)
                    }
                }

                fun JpaSessionContext.findAllPersons() =
                    entityManager.createQuery("FROM PersonEntity", PersonEntity::class.java).resultList

                sessionFactory.runNonTransactionalJpaTask {
                    assertEquals(emptyList(), findAllPersons())
                }

                sessionFactory.runTransactionalJpaTask {
                    entityManager.persistEntity(PersonEntity(1, "Joe"))
                    assertEquals(1, findAllPersons().size)
                }

                sessionFactory.runNonTransactionalJpaTask {
                    assertEquals(1, findAllPersons().size)
                }
            } finally {
                close()
            }
        }
    }
}
