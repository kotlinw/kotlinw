package kotlinw.remoting.client.processor

import kotlin.test.Test
import kotlinw.ksp.testutil.assertCompilationSucceeded
import kotlinw.ksp.testutil.checkCompilationResult
import kotlinw.remoting.processor.RemotingSymbolProcessorProvider

class Test {

    @Test
    fun test() {
        checkCompilationResult(
            """
                        package test
                        
                        import kotlinw.remoting.api.RemotingClient
                        import kotlinw.remoting.api.RemotingCapable
                        import kotlinw.remoting.api.proxy

                        @RemotingCapable
                        interface Service {
                            companion object;

                            suspend fun a()
                        
                            suspend fun convert(value: Int): String
                        }
                        
                        suspend fun test(remotingClient: RemotingClient) {
                            remotingClient.proxy(::Service).a()
                        }
                    """,
            listOf(RemotingSymbolProcessorProvider())
        ) {
            assertCompilationSucceeded()
            println(generatedFiles)
        }
    }
}
