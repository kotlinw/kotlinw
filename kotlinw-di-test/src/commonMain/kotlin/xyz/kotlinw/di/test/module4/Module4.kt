package xyz.kotlinw.di.test.module4

import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.ComponentScan
import xyz.kotlinw.di.api.Module
import xyz.kotlinw.di.test.module3.Formatter

@Module
@ComponentScan
class Module4 {

    @Component
    fun longFormatter() = object: Formatter<Long> {

        override fun supports(value: Any?): Boolean = value is Long

        override fun format(value: Long): String = value.toString()
    }
}
