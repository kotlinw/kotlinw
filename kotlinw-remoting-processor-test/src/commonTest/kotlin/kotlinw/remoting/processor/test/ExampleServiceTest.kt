package kotlinw.remoting.processor.test

import kotlinw.remoting.api.RemotingClient
import kotlinw.remoting.api.proxy
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class ExampleServiceTest {

    @Test
    fun test() {
        val remotingClient = object : RemotingClient {}
        runBlocking {
            val p = remotingClient.proxy(::ExampleServiceClientProxy).helloWorld()
        }
    }
}
