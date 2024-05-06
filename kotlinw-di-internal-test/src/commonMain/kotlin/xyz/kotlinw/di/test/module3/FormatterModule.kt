package xyz.kotlinw.di.test.module3

import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.ComponentScan
import xyz.kotlinw.di.api.Module

@Module
@ComponentScan
class FormatterModule {

    @Component
    fun intFormatter() = object : Formatter<Int> {

        override fun supports(value: Any?): Boolean = value is Int

        override fun format(value: Int): String = value.toString()
    }

    @Component
    fun doubleFormatter() = object : Formatter<Double> {

        override fun supports(value: Any?): Boolean = value is Double

        override fun format(value: Double): String = value.toString()
    }
}
