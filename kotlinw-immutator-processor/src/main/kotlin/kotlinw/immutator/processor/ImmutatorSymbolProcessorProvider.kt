package kotlinw.immutator.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview

class ImmutatorSymbolProcessorProvider : SymbolProcessorProvider {
    @OptIn(KotlinPoetKspPreview::class)
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        ImmutatorSymbolProcessor(
            environment.codeGenerator,
            environment.logger,
            environment.options
        )
}
