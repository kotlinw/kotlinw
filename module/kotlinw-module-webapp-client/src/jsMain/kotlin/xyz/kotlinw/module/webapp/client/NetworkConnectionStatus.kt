package xyz.kotlinw.module.webapp.client

enum class NetworkConnectionStatus(val isConnected: Boolean) {

    Connected(true), NotConnected(false);

    companion object {

        fun of(isOnline: Boolean)= if (isOnline) Connected else NotConnected
    }
}
