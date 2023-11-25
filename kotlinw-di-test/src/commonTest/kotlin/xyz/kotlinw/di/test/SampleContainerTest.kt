package xyz.kotlinw.di.test

import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import xyz.kotlinw.di.api.Scope

class SampleContainerTest {

    @Test
    fun test() = runTest {
        val container = SampleContainer.create()

        var rootScope: Scope? = null
        try {
            rootScope = container.rootScope()
        } finally {
            rootScope?.close()
        }
    }
}
