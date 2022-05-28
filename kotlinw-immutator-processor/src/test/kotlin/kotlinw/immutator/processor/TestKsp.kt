package kotlinw.immutator.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import kotlin.test.Test
import kotlin.test.assertEquals

class TestKsp {
    @Test
    fun test() {
        val personSource = SourceFile.kotlin(
            "Person.kt",
            """
            import kotlinw.immutator.api.Immutate

            @Immutate
            interface Person {
                val name: String
            }
            """
        )
        val result = KotlinCompilation().apply {
            sources = listOf(personSource)
            // useIR = true
            symbolProcessorProviders = listOf(ImmutatorSymbolProcessorProvider())
            messageOutputStream = System.out // see diagnostics in real time
        }.compile()

        println(result.compiledClassAndResourceFiles)

        assertEquals(ExitCode.OK, result.exitCode)
    }
}
