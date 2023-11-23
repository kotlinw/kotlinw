package xyz.kotlinw.di.test

import kotlin.test.Test
import xyz.kotlinw.di.test.module1.Module1

class SampleContainerTest {

    @Test
    fun test() {
        SampleContainer.createInstance().createRootScope(Module1::class)
    }
}
