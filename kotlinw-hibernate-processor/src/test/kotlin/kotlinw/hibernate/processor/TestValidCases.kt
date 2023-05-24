package kotlinw.hibernate.processor

import kotlinw.ksp.testutil.assertCompilationSucceeded
import kotlinw.ksp.testutil.checkCompilationResult
import kotlinw.ksp.testutil.kspGeneratedSources
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestValidCases {

    @Test
    fun testGenerateRegisterPersistentClass() {
        checkCompilationResult(
            """
                import jakarta.persistence.Entity
                import jakarta.persistence.Id
                
                @Entity
                class SimpleEntity(
                    @Id
                    var id: Long
                )
                """,
            listOf(HibernateSymbolProcessorProvider())
        ) {
            assertCompilationSucceeded()

            val generatedFileName = "${HibernateSymbolProcessor.generatedClassSimpleName}.kt"
            assertTrue(kspGeneratedSources.map { it.name }.contains(generatedFileName))
            assertEquals(
                """
                    import kotlin.collections.List
                    import kotlin.collections.listOf
                    import kotlin.reflect.KClass
                    import kotlinw.hibernate.api.configuration.PersistentClassProvider
                    
                    public class GeneratedPackagePersistentClassProvider : PersistentClassProvider {
                      public fun getPersistentClasses(): List<KClass<*>> = listOf(SimpleEntity::class)
                    }
                """.trimIndent().trim(),
                kspGeneratedSources.first { it.name == generatedFileName }.readText().trim()
            )
        }
    }
}
