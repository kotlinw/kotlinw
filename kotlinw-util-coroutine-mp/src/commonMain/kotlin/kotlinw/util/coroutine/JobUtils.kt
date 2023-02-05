package kotlinw.util.coroutine

import kotlinx.coroutines.Job

fun Iterable<Job>.cancelAll() {
    forEach { it.cancel() }
}
