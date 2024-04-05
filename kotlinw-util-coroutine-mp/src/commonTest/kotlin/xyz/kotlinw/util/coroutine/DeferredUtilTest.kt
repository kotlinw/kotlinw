package xyz.kotlinw.util.coroutine

import arrow.core.raise.recover
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration.Companion.seconds
import kotlinw.util.coroutine.awaitSafely
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.completeWith
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

class DeferredUtilTest {

    @Test
    fun testSuccessfulAwaitSafely() = runTest {
        val deferred = CompletableDeferred<Int>()
        deferred.complete(1)
        assertEquals(
            1,
            recover({
                deferred.awaitSafely()
            }, {
                fail()
            })
        )
    }

    @Test
    fun testFailedAwaitSafely() = runTest {
        val deferred = CompletableDeferred<Int>()
        deferred.completeWith(Result.failure(IllegalStateException("error message")))
        recover({
            deferred.awaitSafely()
            fail()
        }, {
            fail()
        }, {
            assertTrue(it is IllegalStateException && it.message == "error message")
        })
    }

    // The same as testFailedAwaitSafely() but using another overload of recover()
    @Test
    fun testFailedAwaitSafely2() = runTest {
        val deferred = CompletableDeferred<Int>()
        deferred.completeWith(Result.failure(IllegalStateException("error message")))
        try {
            recover({
                deferred.awaitSafely()
                fail()
            }, {
                fail()
            })
        } catch (e: Exception) {
            assertTrue(e is IllegalStateException && e.message == "error message")
        }
    }

    @Test
    fun testAwaitSafelyWithCancelledCaller() = runTest {
        val deferred = async(start = UNDISPATCHED) {
            delay(Long.MAX_VALUE)
            1
        }
        val callerJob = async(start = UNDISPATCHED) {
            try {
                recover<_, Int>({
                    deferred.awaitSafely()
                    fail()
                }, {
                    fail()
                })
            } catch (e: Exception) {
                assertTrue(e is CancellationException)
                assertTrue(!currentCoroutineContext().isActive)
                throw e
            }
        }

        delay(1.seconds)
        callerJob.cancelAndJoin()

        assertTrue(callerJob.isCancelled)
        assertTrue(callerJob.getCompletionExceptionOrNull() is CancellationException)

        assertTrue(deferred.isActive)
        deferred.cancel()
    }

    @Test
    fun testAwaitSafelyWithCancelledDeferred() = runTest {
        val deferred = async(start = UNDISPATCHED) {
            println(1)
            delay(Long.MAX_VALUE)
            1
        }
        val callerJob = async (start = UNDISPATCHED) {
            val result = recover<_, Int>({
                deferred.awaitSafely()
            }, {
                2
            }, {
                fail()
            })
            assertEquals(2, result)

            delay(Long.MAX_VALUE)
        }

        delay(1.seconds)
        deferred.cancelAndJoin()

        assertTrue(deferred.isCancelled)
        assertTrue(deferred.getCompletionExceptionOrNull() is CancellationException)

        assertTrue(callerJob.isActive)
        callerJob.cancel()
    }
}
