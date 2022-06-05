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
            assertHasKspError("Person.kt:4: Only interfaces are allowed to be annotated with @Immutate.")
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
            assertHasKspError("TODO") // TODO
        }
    }
}
