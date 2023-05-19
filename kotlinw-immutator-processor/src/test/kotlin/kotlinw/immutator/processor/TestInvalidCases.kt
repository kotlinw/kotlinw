package kotlinw.immutator.processor

import com.tschuchort.compiletesting.SourceFile
import kotlinw.ksp.testutil.assertCompilationFailed
import kotlinw.ksp.testutil.assertHasKspError
import kotlinw.ksp.testutil.checkCompilationResult
import kotlin.test.Test

class TestInvalidCases {
    @Test
    fun testAnnotatedClassMustBeInterface() {
        checkCompilationResult(
            SourceFile.kotlin(
                "Person.kt",
                """
                        import kotlinw.immutator.annotation.Immutate
            
                        @Immutate
                        class Person
                        """
            ),
            listOf(ImmutatorSymbolProcessorProvider())
        ) {
            assertCompilationFailed()
            assertHasKspError("Only interface declarations are supported by @Immutate.", "Person.kt:4")
        }
    }

    @Test
    fun testInvalidPropertyType() {
        checkCompilationResult(
            """
                import kotlinw.immutator.annotation.Immutate
                
                data class Data(var s: String)
                
                @Immutate
                sealed interface TestCase {
                    
                    companion object
                    
                    val d: Data
                }
            """,
            listOf(ImmutatorSymbolProcessorProvider())
        ) {
            assertCompilationFailed()
            assertHasKspError("Property has type that is not supported by @Immutate.", "Test.kt:10")
            assertHasKspError(
                "Only properties of supported types are allowed in interfaces annotated with @Immutate.",
                "Test.kt:6"
            )
        }
    }
}
