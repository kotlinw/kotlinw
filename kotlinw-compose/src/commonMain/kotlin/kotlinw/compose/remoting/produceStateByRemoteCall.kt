package kotlinw.compose.remoting

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import kotlinw.remoting.api.RemoteService
import kotlinw.remoting.api.Remoting
import kotlinw.remoting.api.callRemote
import kotlinw.remoting.api.ApplicationNodeId
import kotlin.reflect.KSuspendFunction2

// TODO FailedDataFetch
sealed interface DataFetchState<Response>

class DataFetchInProgress<Response> : DataFetchState<Response>

data class DataFetchSuccessful<Response>(val response: Response) : DataFetchState<Response>

// TODO util-ba
@Composable
@Deprecated(message = "see: kotlinw.remoting.api.Remoting.produceRemoteCallState()")
inline fun <reified T : RemoteService, reified Request : Any, reified Response : Any> Remoting.produceStateByRemoteCall(
    nodeId: ApplicationNodeId,
    method: KSuspendFunction2<T, Request, Response>,
    request: Request
): State<DataFetchState<Response>> {
    return produceState<DataFetchState<Response>>(DataFetchInProgress()) {
        val response = callRemote(nodeId, method, request) // TODO hibakez.
        value = DataFetchSuccessful(response)
    }
}
