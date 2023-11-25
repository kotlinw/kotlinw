package xyz.kotlinw.di.test.module2

import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.test.module3.GenericFormatter

interface Service3 {
}

@Component
class Service3Impl(private val genericFormatter: GenericFormatter): Service3 {
}
