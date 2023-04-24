package kotlinw.remoting.client.processor

import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.COMPILATION_ERROR
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.OK
import com.tschuchort.compiletesting.KotlinCompilation.Result
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import kotlinw.remoting.processor.RemotingSymbolProcessorProvider
import kotlin.test.assertEquals
import kotlin.test.assertTrue

fun Result.assertCompilationFailed() {
    assertEquals(COMPILATION_ERROR, exitCode)
}

fun Result.assertCompilationSucceeded() {
    assertEquals(OK, exitCode)
}

fun KotlinCompilation.Result.assertHasKspError(message: String) {
    assertTrue {
        messages.lines().any {
            Regex("e: \\[ksp] .*?$message").matches(it)
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
