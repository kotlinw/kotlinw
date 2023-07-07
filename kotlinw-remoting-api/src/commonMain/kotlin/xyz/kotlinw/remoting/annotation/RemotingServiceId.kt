package xyz.kotlinw.remoting.annotation

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
annotation class RemotingServiceId(val value: String)
