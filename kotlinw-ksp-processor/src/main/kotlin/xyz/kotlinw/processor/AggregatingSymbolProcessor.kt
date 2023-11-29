package xyz.kotlinw.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import xyz.kotlinw.di.processor.DiSymbolProcessor

class AggregatingSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val kspLogger: KSPLogger,
    private val kspOptions: Map<String, String>
): SymbolProcessor {

    private val diProcessor = DiSymbolProcessor(codeGenerator, kspLogger, kspOptions)

    override fun process(resolver: Resolver): List<KSAnnotated> {
        return diProcessor.process(resolver)
    }
}
