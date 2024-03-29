package xyz.kotlinw.di.test.module5

import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.OnConstruction
import xyz.kotlinw.di.api.OnTerminate
import xyz.kotlinw.di.test.module1.Service1

interface Service4 {
}

@Component
class Service4Impl(service1: Service1) : Service4 {

    @OnConstruction
    fun onConstruct() {}

    @OnTerminate
    fun onTerminate() {}
}
