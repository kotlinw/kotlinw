package xyz.kotlinw.di.test

import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import xyz.kotlinw.di.api.ContainerScope

class SampleContainerTest {

    @Test
    fun test() = runTest {
        val container = SampleContainer.create()

        var rootScope: ContainerScope? = null
        try {
            rootScope = container.rootScope()
        } finally {
            rootScope?.close()
        }
    }
}
