package kotlinw.util.coroutine

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.kotlinFunction

interface ProxyInvocationHandlerPeer {

    companion object {

        inline fun <reified T : ProxyInvocationHandlerPeer> proxyEquals(
            other: Any?,
            peersEqual: (T) -> Boolean
        ): Boolean =
            when {
                other == null -> false
                other == this -> true
                !Proxy.isProxyClass(other::class.java) -> false
                else -> {
                    val invocationHandler = Proxy.getInvocationHandler(other)
                    if (invocationHandler is ProxyInvocationHandler) {
                        val peer = invocationHandler.peer
                        if (peer is T) {
                            peersEqual(peer)
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                }
            }
    }

    fun invokeToString(): String

    fun invokeHashCode(): Int

    fun invokeEquals(other: Any?): Boolean

    fun invokeKotlinNormalFunction(kotlinFunction: KFunction<*>, vararg args: Any?): Any?

    suspend fun invokeKotlinSuspendFunction(kotlinFunction: KFunction<*>, vararg args: Any?): Any?

    fun invokeJavaMethod(method: Method, vararg args: Any?): Any?
}

// TODO property-k esetén rendes hibaüzenetet
// TODO legyen egy opcionális szinkronizálás, amikor alapesetben csak egy hívás futhat egyszerre
class ProxyInvocationHandler(
    val peer: ProxyInvocationHandlerPeer
) : InvocationHandler {

    override fun invoke(proxy: Any, method: Method, args: Array<out Any?>?): Any? {
        val kotlinFunction = method.kotlinFunction
        val notNullArgs = args ?: emptyArray()
        return when {
            kotlinFunction == null -> peer.invokeJavaMethod(method, *notNullArgs)

            kotlinFunction.isSuspend -> {
                val lastArg = notNullArgs.last()
                val continuation = lastArg as Continuation<*>
                val finalArgs = notNullArgs.dropLast(1).toTypedArray()
                invokeSuspendFunction(continuation) {
                    peer.invokeKotlinSuspendFunction(kotlinFunction, *finalArgs)
                }
            }

            else -> {
                when (kotlinFunction) {
                    Any::equals -> peer.invokeEquals(notNullArgs[0])
                    Any::hashCode -> peer.invokeHashCode()
                    Any::toString -> peer.invokeToString()
                    else -> peer.invokeKotlinNormalFunction(kotlinFunction, *notNullArgs)
                }
            }
        }
    }
}
