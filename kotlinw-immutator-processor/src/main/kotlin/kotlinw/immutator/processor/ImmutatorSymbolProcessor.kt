package kotlinw.immutator.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import kotlinw.immutator.api.Immutate

class ImmutatorSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {
    private val annotationQualifiedName = Immutate::class.qualifiedName!!

    private val annotationSimpleName = Immutate::class.simpleName!!

    private val annotationDisplayName = "@$annotationSimpleName"

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver
            .getSymbolsWithAnnotation(annotationQualifiedName)
            .toList()

        val invalidSymbols = symbols.filter { !it.validate() }.toList()

        symbols
            .filter { it.validate() }
            .forEach { annotatedSymbol ->
                if (annotatedSymbol is KSClassDeclaration) {
                    processClassDeclaration(annotatedSymbol)
                } else {
                    logger.error("Unsupported annotated symbol: $annotatedSymbol", annotatedSymbol)
                }
            }

        return invalidSymbols
    }

    private fun processClassDeclaration(classDeclaration: KSClassDeclaration) {
        logger.error("<<< " + classDeclaration.isImmutable, classDeclaration)
    }
}
