package xyz.kotlinw.module.webapp.client

// TODO ez egy belső állapot legyen, kifelé nincs értelme megkülönböztetni az UpdatingStatus-t
enum class NetworkConnectionStatus(val isConnected: Boolean) {

    Connected(true), NotConnected(false), UpdatingStatus(false);

    companion object {

        fun of(isOnline: Boolean)= if (isOnline) Connected else NotConnected
    }
}
