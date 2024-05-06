package xyz.kotlinw.di.test.module3

interface Formatter<in T: Any?> {

    fun supports(value: Any?): Boolean

    fun format(value: T): String
}
