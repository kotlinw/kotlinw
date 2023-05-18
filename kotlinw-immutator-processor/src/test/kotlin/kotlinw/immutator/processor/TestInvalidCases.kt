package kotlinw.immutator.processor

import com.tschuchort.compiletesting.SourceFile
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
            )
        ) {
            assertCompilationFailed()
            assertHasKspError("Person.kt:4: Only interface declarations are supported by @Immutate.")
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
                    val d: Data
                }
            """
        ) {
            assertCompilationFailed()
            assertHasKspError("Test.kt:7: Property has type that is not supported by @Immutate.")
            assertHasKspError("Test.kt:6: Only properties of supported types are allowed in interfaces annotated with @Immutate.")
        }
    }
}
