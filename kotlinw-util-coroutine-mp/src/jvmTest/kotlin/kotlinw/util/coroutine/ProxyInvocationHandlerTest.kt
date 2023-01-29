package kotlinw.util.coroutine

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class ProxyInvocationHandlerTest {

    interface TestInterface {

        fun normalFunction(): Int

        fun normalFunctionThrowingException(): Int

        suspend fun suspendFunction(): String

        suspend fun suspendFunctionThrowingException(): String
    }

    data class TestImplementation(val value: Int) : TestInterface {

        override fun normalFunction(): Int = value

        override fun normalFunctionThrowingException(): Int {
            throw IllegalStateException()
        }

        override suspend fun suspendFunction(): String {
            yield()
            delay(1)
            return value.toString()
        }

        override suspend fun suspendFunctionThrowingException(): String {
            yield()
            delay(1)
            throw IllegalStateException()
        }
    }

    class TestProxyInvocationHandler(private val implementation: TestImplementation) : ProxyInvocationHandler() {

        override fun invokeToString(): String = implementation.toString()

        override fun invokeHashCode(): Int = implementation.hashCode()

        override fun invokeEquals(other: Any?): Boolean =
            when {
                other == null -> false
                other == this -> true
                !Proxy.isProxyClass(other::class.java) -> false
                else -> {
                    val otherInvocationHandler = Proxy.getInvocationHandler(other)
                    if (otherInvocationHandler is TestProxyInvocationHandler) {
                        otherInvocationHandler.implementation.value == implementation.value
                    } else {
                        false
                    }
                }
            }

        override fun invokeKotlinNormalFunction(kotlinFunction: KFunction<*>, vararg args: Any?): Any? {
            assertTrue(args.isEmpty())
            return kotlinFunction.call(implementation, *args)
        }

        override suspend fun invokeKotlinSuspendFunction(
            kotlinFunction: KFunction<*>,
            vararg args: Any?
        ): Any? {
            assertTrue(args.last() is Continuation<*>)
            return kotlinFunction.callSuspend(implementation, *(args.dropLast(1).toTypedArray()))
        }

        override fun invokeJavaMethod(method: Method, vararg args: Any?): Any? {
            fail()
        }
    }

    @Test
    fun testAnyMethods() {
        val proxy1a = Proxy.newProxyInstance(
            this::class.java.classLoader,
            arrayOf(TestInterface::class.java),
            TestProxyInvocationHandler(TestImplementation(1))
        )
        val proxy1b = Proxy.newProxyInstance(
            this::class.java.classLoader,
            arrayOf(TestInterface::class.java),
            TestProxyInvocationHandler(TestImplementation(1))
        )
        val proxy2 = Proxy.newProxyInstance(
            this::class.java.classLoader,
            arrayOf(TestInterface::class.java),
            TestProxyInvocationHandler(TestImplementation(2))
        )

        assertEquals(proxy1a, proxy1b)
        assertNotEquals(proxy1a, proxy2)

        assertEquals(proxy1a.hashCode(), proxy1b.hashCode())
        assertNotEquals(proxy1a.hashCode(), proxy2.hashCode())

        assertEquals(proxy1a.toString(), proxy1b.toString())
        assertNotEquals(proxy1a.toString(), proxy2.toString())
    }

    @Test
    fun testCallKotlinNormalFunctions() {
        val proxy1 = Proxy.newProxyInstance(
            this::class.java.classLoader,
            arrayOf(TestInterface::class.java),
            TestProxyInvocationHandler(TestImplementation(1))
        ) as TestInterface

        assertEquals(1, proxy1.normalFunction())
        assertFailsWith<IllegalStateException> { proxy1.normalFunctionThrowingException() }
    }

    @Test
    fun testCallKotlinSuspendFunctions() {
        val proxy1 = Proxy.newProxyInstance(
            this::class.java.classLoader,
            arrayOf(TestInterface::class.java),
            TestProxyInvocationHandler(TestImplementation(1))
        ) as TestInterface

        runTest {
            assertEquals("1", proxy1.suspendFunction())
            assertFailsWith<IllegalStateException> { proxy1.suspendFunctionThrowingException() }
        }
    }
}
