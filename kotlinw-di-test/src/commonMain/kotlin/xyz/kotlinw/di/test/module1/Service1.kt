package xyz.kotlinw.di.test.module1

import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.test.module2.Service3

interface Service1 {
}

@Component
class Service1Impl(private val service3: Service3): Service1 {
}
