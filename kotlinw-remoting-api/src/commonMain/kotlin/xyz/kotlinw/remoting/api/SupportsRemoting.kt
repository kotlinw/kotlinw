package xyz.kotlinw.remoting.api

/**
 * Interfaces annotated by `@SupportsRemoting` are used as communication protocol definitions in remote communication.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
annotation class SupportsRemoting
