package xyz.kotlinw.di.api.internal

interface ComponentImplementorInternal<T: Any> {

    fun createComponentProxy(): Any

    fun createComponentInstance(): T

    val postConstruct: ((T) -> Unit)?

    val preDestroy: ((T) -> Unit)?
}
