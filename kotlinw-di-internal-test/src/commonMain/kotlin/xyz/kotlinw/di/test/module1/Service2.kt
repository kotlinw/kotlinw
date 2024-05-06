package xyz.kotlinw.di.test.module1

import xyz.kotlinw.di.api.Component

interface Service2 {
}

class Service2Impl(service1: Service1) : Service2 {

}
