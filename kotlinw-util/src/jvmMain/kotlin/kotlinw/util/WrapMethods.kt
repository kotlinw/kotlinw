package kotlinw.util

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation
import kotlin.reflect.KClass
import kotlin.reflect.jvm.kotlinFunction

interface AroundProxyExecutionContextProvider {
    suspend fun <T> withExecutionContext(block: suspend () -> T): T
}

fun <T : Any> wrapMethods(
    coroutineInterface: KClass<T>,
    obj: T,
    classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
    executionContextProvider: AroundProxyExecutionContextProvider
): T =
    Proxy.newProxyInstance(
        classLoader,
        arrayOf(coroutineInterface.java),
        WrapMethodsProxyInvocationHandler(coroutineInterface, obj, executionContextProvider)
    ) as T

private class SuspendInvocationHelper(
    private val executionContextProvider: AroundProxyExecutionContextProvider,
    private val targetMethodInvoker: () -> Any?
) {
    companion object {
        val callMethod: Method by lazy {
            SuspendInvocationHelper::class.java.getMethod(
                SuspendInvocationHelper::call.name,
                Continuation::class.java
            )
        }
    }

    suspend fun call(): Any? = executionContextProvider.withExecutionContext { targetMethodInvoker() }
}

private class WrapMethodsProxyInvocationHandler<T : Any>(
    val coroutineInterface: KClass<T>,
    val obj: T,
    val executionContextProvider: AroundProxyExecutionContextProvider
) : InvocationHandler {
    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        require(args != null)
        val targetMethod = coroutineInterface.java.getMethod(method.name, *method.parameterTypes)
        val kotlinFunction = method.kotlinFunction
        return if (kotlinFunction != null && kotlinFunction.isSuspend) {
            val helper = SuspendInvocationHelper(executionContextProvider) { targetMethod.invoke(obj, *args) }
            SuspendInvocationHelper.callMethod.invoke(helper, args.last())
        } else {
            targetMethod.invoke(obj, *args)
        }
    }
}
