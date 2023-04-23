package kotlinw.remoting.processor.test

import kotlinw.remoting.api.RemotingCapable

@RemotingCapable
interface ExampleService {

    suspend fun helloWorld(): String
}
