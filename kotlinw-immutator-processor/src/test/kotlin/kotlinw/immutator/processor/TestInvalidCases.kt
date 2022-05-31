package kotlinw.immutator.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.KotlinCompilation.Result
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestInvalidCases {
    @Test
    fun testAnnotatedClassMustBeInterface() {
        checkCompilationResult(
            SourceFile.kotlin(
                "Person.kt",
                """
                        import kotlinw.immutator.api.Immutate
            
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
    fun testInterfaceMustBeSealed() {
        checkCompilationResult(
            SourceFile.kotlin(
                "Person.kt",
                """
                        import kotlinw.immutator.api.Immutate
            
                        @Immutate
                        interface Person
                        """
            )
        ) {
            assertCompilationFailed()
            assertHasKspError("Person.kt:4: An interface annotated with @Immutate must be sealed.")
        }
    }

    private fun Result.assertCompilationFailed() {
        assertEquals(ExitCode.COMPILATION_ERROR, exitCode)
    }

    private fun KotlinCompilation.Result.assertHasKspError(message: String) {
        assertTrue {
            messages.lines().any {
                Regex("e: \\[ksp] .*?$message").matches(it)
            }
        }
    }

    private inline fun checkCompilationResult(sourceFile: SourceFile, block: KotlinCompilation.Result.() -> Unit) {
        block(compile(sourceFile))
    }

    private fun compile(sourceFile: SourceFile) = KotlinCompilation().apply {
        sources = listOf(sourceFile)
        symbolProcessorProviders = listOf(ImmutatorSymbolProcessorProvider())
        messageOutputStream = System.out
        inheritClassPath = true
    }.compile()
}
