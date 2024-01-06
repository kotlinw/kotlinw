package xyz.kotlinw.remoting.api

enum class RemoteServiceBackend {
    WebRequest, WebSocket
}

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
annotation class RemoteServiceSet(val backend: RemoteServiceBackend)
