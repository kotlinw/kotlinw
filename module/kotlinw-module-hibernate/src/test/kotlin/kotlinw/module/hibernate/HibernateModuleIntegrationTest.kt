package kotlinw.module.hibernate

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import kotlinw.hibernate.api.configuration.PersistentClassProvider
import kotlinw.hibernate.core.api.jdbcTask
import kotlinw.hibernate.core.api.runReadOnlyJpaTask
import kotlinw.hibernate.core.api.runTransactionalJpaTask
import kotlinw.hibernate.core.entity.JpaSessionContext
import kotlinw.hibernate.core.schemaexport.ExportedSchemaScriptType
import kotlinw.hibernate.core.schemaexport.HibernateSqlSchemaExporter
import kotlinw.jdbc.util.executeStatements
import kotlinw.koin.core.api.startKoin
import kotlinw.module.hibernate.core.hibernateModule
import org.hibernate.SessionFactory
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals

@Entity
@Table(name = "Person")
class PersonEntity(

    @Id
    var id: Long,

    var name: String
)

class HibernateModuleIntegrationTest {

    @Test
    fun testBootstrap() {
        val koinApplication = startKoin {
            modules(
                hibernateModule,
                module {
                    single {
                        PersistentClassProvider {
                            listOf(PersonEntity::class)
                        }
                    }
                }
            )
        }

        try {
            with(koinApplication.koin) {
                val sqlSchemaExporter = get<HibernateSqlSchemaExporter>()

                val createSchemaScript = sqlSchemaExporter.exportSchema(ExportedSchemaScriptType.Create)
                assertEquals(
                    """
                    create table Person (id bigint not null, name varchar(255), primary key (id));
                    """.trimIndent(),
                    createSchemaScript
                )

                val sessionFactory = get<SessionFactory>()

                sessionFactory.runTransactionalJpaTask {
                    jdbcTask {
                        executeStatements(createSchemaScript)
                    }
                }

                fun JpaSessionContext.findAllPersons() =
                    entityManager.createQuery("FROM PersonEntity", PersonEntity::class.java).resultList

                sessionFactory.runReadOnlyJpaTask {
                    assertEquals(emptyList(), findAllPersons())
                }

                sessionFactory.runTransactionalJpaTask {
                    entityManager.persistEntity(PersonEntity(1, "Joe"))
                    assertEquals(1, findAllPersons().size)
                }

                sessionFactory.runReadOnlyJpaTask {
                    assertEquals(1, findAllPersons().size)
                }
            }
        } finally {
            koinApplication.close()
        }
    }
}
