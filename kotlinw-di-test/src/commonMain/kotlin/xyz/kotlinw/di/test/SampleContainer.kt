package xyz.kotlinw.di.test

import xyz.kotlinw.di.api.Container
import xyz.kotlinw.di.test.module1.Module1

@Container(Module1::class)
class SampleContainer {

    companion object
}
