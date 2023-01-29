package kotlinw.util.coroutine

import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.UndeclaredThrowableException
import kotlin.coroutines.Continuation
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.kotlinFunction

abstract class ProxyInvocationHandler : InvocationHandler {

    final override fun invoke(proxy: Any, method: Method, args: Array<out Any?>?): Any? =
        try {
            val kotlinFunction = method.kotlinFunction
            val notNullArgs = args ?: emptyArray()
            when {
                kotlinFunction == null -> invokeJavaMethod(method, *notNullArgs)

                kotlinFunction.isSuspend -> {
                    val lastArg = notNullArgs.last()
                    val continuation = lastArg as Continuation<*>
                    invokeSuspendFunction(continuation) {
                        invokeKotlinSuspendFunction(kotlinFunction, *notNullArgs)
                    }
                }

                else -> {
                    when (kotlinFunction) {
                        Any::equals -> invokeEquals(notNullArgs[0])
                        Any::hashCode -> invokeHashCode()
                        Any::toString -> invokeToString()
                        else -> invokeKotlinNormalFunction(kotlinFunction, *notNullArgs)
                    }
                }
            }
        } catch (e: InvocationTargetException) {
            throw e.cause!!
        }

    protected abstract fun invokeToString(): String

    protected abstract fun invokeHashCode(): Int

    protected abstract fun invokeEquals(other: Any?): Boolean

    protected abstract fun invokeKotlinNormalFunction(kotlinFunction: KFunction<*>, vararg args: Any?): Any?

    protected abstract suspend fun invokeKotlinSuspendFunction(kotlinFunction: KFunction<*>, vararg args: Any?): Any?

    protected abstract fun invokeJavaMethod(method: Method, vararg args: Any?): Any?
}
