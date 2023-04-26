// TODO move to test source set
package kotlinw.remoting.core

import kotlinw.remoting.api.SupportsRemoting

@SupportsRemoting
interface EchoService {

    suspend fun echo(message: String): String
}
