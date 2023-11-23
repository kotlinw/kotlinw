package xyz.kotlinw.di.test.module2

import xyz.kotlinw.di.api.ComponentScan
import xyz.kotlinw.di.api.Module
import xyz.kotlinw.di.api.Component

@Module
@ComponentScan
abstract class Module2 {

    @Component
    fun service2(): Service3 = Service3Impl()
}
