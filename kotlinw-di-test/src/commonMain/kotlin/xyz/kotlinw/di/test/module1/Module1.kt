package xyz.kotlinw.di.test.module1

import xyz.kotlinw.di.api.ComponentScan
import xyz.kotlinw.di.api.Module
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.test.module2.Module2

@Module(includeModules = [Module2::class])
@ComponentScan
class Module1 {

    @Component
    fun service2(service1: Service1): Service2 = Service2Impl(service1)
}
