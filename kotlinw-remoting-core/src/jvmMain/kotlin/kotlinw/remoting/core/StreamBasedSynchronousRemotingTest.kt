// TODO move to test source set
package kotlinw.remoting.core

import kotlinw.remoting.api.SupportsRemoting

@SupportsRemoting
interface EchoService {

    companion object;

    suspend fun echo(message: String): String
}
