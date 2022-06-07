package kotlinw.statemachine.dot.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.validate
import kotlinw.statemachine.dot.annotation.GenerateDot

class DotGeneratorException(override val message: String, val ksNode: KSNode, override val cause: Throwable? = null) :
    RuntimeException(message, cause)

internal val annotationQualifiedName = GenerateDot::class.qualifiedName!!

class DotGeneratorSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {
    private val annotationSimpleName = GenerateDot::class.simpleName!!

    private val annotationDisplayName = "@$annotationSimpleName"

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver
            .getSymbolsWithAnnotation(annotationQualifiedName)
            .toList()

        symbols
            .filter { it !is KSClassDeclaration || it.qualifiedName == null }
            .forEach {
                logger.error("Invalid annotated element, expected class declaration with qualified name: $it")
            }

        val validSymbols = symbols
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.qualifiedName != null }
            .filter { it.validate() }
            .toList()

        val invalidSymbols = symbols.filter { !it.validate() }.toList()

        validSymbols.forEach { symbol ->
            try {
                processClassDeclaration(symbol)
            } catch (e: DotGeneratorException) {
                logger.error(e.message, e.ksNode)
            } catch (e: Exception) {
                logger.error("Internal processing error: ${e.message}", symbol)
            }
        }

        return invalidSymbols
    }

    private fun processClassDeclaration(ksClassDeclaration: KSClassDeclaration) {
        // TODO
    }
}
