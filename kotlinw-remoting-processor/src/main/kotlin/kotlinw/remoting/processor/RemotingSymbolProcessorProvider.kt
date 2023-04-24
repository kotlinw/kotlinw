package kotlinw.remoting.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class RemotingSymbolProcessorProvider  : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        RemotingSymbolProcessor(
            environment.codeGenerator,
            environment.logger,
            environment.options
        )
}
