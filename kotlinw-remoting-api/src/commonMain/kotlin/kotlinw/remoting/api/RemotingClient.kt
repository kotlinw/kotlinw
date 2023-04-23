package kotlinw.remoting.api

interface RemotingClient

inline fun <reified T : Any, reified P : ClientProxy<T>> RemotingClient.proxy(factory: (RemotingClient) -> P): T =
    factory(this) as T

interface ClientProxy<T : Any>
