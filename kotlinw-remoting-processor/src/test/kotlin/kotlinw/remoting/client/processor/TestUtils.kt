package kotlinw.remoting.client.processor

import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.COMPILATION_ERROR
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.OK
import com.tschuchort.compiletesting.KotlinCompilation.Result
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import kotlinw.remoting.processor.RemotingSymbolProcessorProvider
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

fun Result.assertCompilationFailed() {
    assertEquals(COMPILATION_ERROR, exitCode)
}

fun Result.assertCompilationSucceeded() {
    assertEquals(OK, exitCode)
}

private val kspErrorLineRegex = Regex("""e: \[ksp] (.+?)/sources/(.+?:\d+): (.+?)""")

fun KotlinCompilation.Result.assertHasKspError(message: String, location: String? = null) {
    assertTrue {
        messages.lines().any {
            if (it.startsWith("e: [ksp] ")) {
                kspErrorLineRegex.matchEntire(it)?.let { matchResult ->
                    val currentLocation = matchResult.groupValues.get(2)
                    val locationMatch = location == null || location == currentLocation

                    val currentMessage = matchResult.groupValues.get(3)
                    val messageMatch = message == currentMessage

                    messageMatch && locationMatch
                } ?: false
            } else {
                false
            }
        }
    }
}

inline fun checkCompilationResult(sourceFile: SourceFile, block: KotlinCompilation.Result.() -> Unit) {
    block(compile(sourceFile))
}

inline fun checkCompilationResult(sourceFile: String, block: KotlinCompilation.Result.() -> Unit) {
    block(compile(SourceFile.kotlin("Test.kt", sourceFile)))
}

fun compile(sourceFile: SourceFile, vararg additionalSymbolProcessorProviders: SymbolProcessorProvider) =
    KotlinCompilation().apply {
        sources = listOf(sourceFile)
        symbolProcessorProviders = listOf(RemotingSymbolProcessorProvider()).plus(additionalSymbolProcessorProviders)
        messageOutputStream = System.out
        inheritClassPath = true
    }.compile()

internal val KotlinCompilation.Result.workingDir: File
    get() =
        outputDirectory.parentFile!!

val KotlinCompilation.Result.kspGeneratedSources: List<File>
    get() {
        val kspWorkingDir = workingDir.resolve("ksp")
        val kspGeneratedDir = kspWorkingDir.resolve("sources")
        val kotlinGeneratedDir = kspGeneratedDir.resolve("kotlin")
        val javaGeneratedDir = kspGeneratedDir.resolve("java")
        return kotlinGeneratedDir.walkTopDown().toList() +
                javaGeneratedDir.walkTopDown()
    }
