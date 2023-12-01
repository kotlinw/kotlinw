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

            var nestedScope: NestedScope? = null
            try {
                nestedScope = container.nestedScope(rootScope)
                // ..
            } finally {
                nestedScope?.close()
            }

            var nestedScope2: NestedScope? = null
            try {
                nestedScope2 = container.nestedScopeWithExternalComponent(rootScope, ExternalComponent)
                // ..
            } finally {
                nestedScope2?.close()
            }
        } finally {
            rootScope?.close()
        }
    }
}
