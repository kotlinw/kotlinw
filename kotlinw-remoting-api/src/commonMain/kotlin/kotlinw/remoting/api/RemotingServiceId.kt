package kotlinw.remoting.api

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
annotation class RemotingServiceId(val value: String)
