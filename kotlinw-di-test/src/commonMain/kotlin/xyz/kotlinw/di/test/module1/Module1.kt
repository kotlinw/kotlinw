package xyz.kotlinw.di.test.module1

import xyz.kotlinw.di.api.ComponentScan
import xyz.kotlinw.di.api.Module

@Module
@ComponentScan
class Module1 {

    fun service2(service1: Service1): Service2 = Service2Impl(service1)
}
