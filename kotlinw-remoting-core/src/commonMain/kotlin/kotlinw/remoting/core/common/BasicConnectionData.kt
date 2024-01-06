package kotlinw.remoting.core.common

interface BasicConnectionData {

    val connectionId: RemoteConnectionId

    val principal: Any?
}
