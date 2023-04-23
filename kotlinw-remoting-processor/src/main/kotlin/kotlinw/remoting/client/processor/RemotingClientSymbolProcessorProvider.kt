package kotlinw.remoting.client.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class RemotingClientSymbolProcessorProvider  : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        RemotingClientSymbolProcessor(
            environment.codeGenerator,
            environment.logger,
            environment.options
        )
}
