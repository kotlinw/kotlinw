package kotlinw.remoting.api.client

interface RemotingClient

inline fun <reified T : Any, reified P : ClientProxy<T>> RemotingClient.proxy(factory: (RemotingClient) -> P): T =
    factory(this) as T
