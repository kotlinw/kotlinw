package kotlinw.statemachine.dot.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class DotGeneratorSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        DotGeneratorSymbolProcessor(
            environment.codeGenerator,
            environment.logger,
            environment.options
        )
}
