package kotlinw.remoting.core.common

import xyz.kotlinw.remoting.api.MessagingPeerId
import xyz.kotlinw.remoting.api.MessagingConnectionId

interface BasicConnectionData {

    val remotePeerId: MessagingPeerId

    val connectionId: MessagingConnectionId
}