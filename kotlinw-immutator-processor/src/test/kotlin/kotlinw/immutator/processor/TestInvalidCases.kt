package kotlinw.immutator.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestInvalidCases {
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
            assertEquals(ExitCode.COMPILATION_ERROR, exitCode)
            assertTrue {
                messages.lines().any {
                    Regex(""".*?Person.kt:4: An interface annotated with @Immutate must be sealed.""").matches(it)
                }
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
