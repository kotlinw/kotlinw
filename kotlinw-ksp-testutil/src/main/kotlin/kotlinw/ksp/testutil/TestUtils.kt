@file:OptIn(ExperimentalCompilerApi::class)

package kotlinw.ksp.testutil

import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.COMPILATION_ERROR
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.OK
import com.tschuchort.compiletesting.KotlinCompilation.Result
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

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

inline fun checkCompilationResult(sourceFile: SourceFile, kspProcessorProviders: List<SymbolProcessorProvider>, block: KotlinCompilation.Result.() -> Unit) {
    block(compile(sourceFile, kspProcessorProviders))
}

inline fun checkCompilationResult(sourceFile: String, kspProcessorProviders: List<SymbolProcessorProvider>, block: KotlinCompilation.Result.() -> Unit) {
    block(compile(SourceFile.kotlin("Test.kt", sourceFile), kspProcessorProviders))
}

fun compile(sourceFile: SourceFile, kspProcessorProviders: List<SymbolProcessorProvider>) =
    KotlinCompilation().apply {
        sources = listOf(sourceFile)
        symbolProcessorProviders = kspProcessorProviders.toList()
        messageOutputStream = System.out
        inheritClassPath = true
        kotlincArguments += "-Xskip-prerelease-check"
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
