package kotlinw.module.hibernate

import jakarta.persistence.Entity
import jakarta.persistence.EntityManager
import jakarta.persistence.Id
import jakarta.persistence.Table
import kotlinw.hibernate.core.api.createTypeSafeEntityManager
import kotlinw.hibernate.core.api.jdbcTask
import kotlinw.hibernate.core.api.jpaTask
import kotlinw.hibernate.core.api.transactional
import kotlinw.hibernate.core.schemaexport.ExportedSchemaScriptType
import kotlinw.hibernate.core.schemaexport.HibernateSqlSchemaExporter
import kotlinw.jdbc.util.executeStatements
import org.hibernate.SessionFactory
import org.koin.core.context.startKoin
import kotlin.test.Test
import kotlin.test.assertEquals

@Entity
@Table(name = "Person")
class PersonEntity(

    @Id
    var id: Long,

    var name: String
)

class HibernateModuleTest {

    @Test
    fun testBootstrap() {
        val koinApplication = startKoin {
            modules(hibernateModule(PersonEntity::class))
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

                sessionFactory.createTypeSafeEntityManager().use {
                    it.jdbcTask {
                        executeStatements(createSchemaScript)
                    }
                }

                fun EntityManager.findAllPersons() =
                    createQuery("FROM PersonEntity", PersonEntity::class.java).resultList

                sessionFactory.jpaTask {
                    assertEquals(emptyList(), findAllPersons())
                }

                sessionFactory.jpaTask {
                    transactional {
                        persistEntity(PersonEntity(1, "Joe"))
                    }

                    assertEquals(1, findAllPersons().size)
                }
            }
        } finally {
            koinApplication.close()
        }
    }
}
