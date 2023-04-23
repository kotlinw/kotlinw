package kotlinw.remoting.client.processor

import kotlin.test.Test

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
                        
                            suspend fun a()
                        
                            suspend fun convert(value: Int): String
                        }
                        
                        suspend fun test(remotingClient: RemotingClient) {
                            remotingClient.proxy(::Service).a()
                        }
                    """
        ) {
            assertCompilationSucceeded()
            println(generatedFiles)
        }
    }
}
