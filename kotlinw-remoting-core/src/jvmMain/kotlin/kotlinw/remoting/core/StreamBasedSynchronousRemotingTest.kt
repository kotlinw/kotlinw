// TODO move to test source set
package kotlinw.remoting.core

import xyz.kotlinw.remoting.annotation.SupportsRemoting

@SupportsRemoting
interface EchoService {

    companion object;

    suspend fun echo(message: String): String
}
