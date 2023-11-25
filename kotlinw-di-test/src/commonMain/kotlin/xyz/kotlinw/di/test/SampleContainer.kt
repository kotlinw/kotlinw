package xyz.kotlinw.di.test

import xyz.kotlinw.di.api.ComponentQuery
import xyz.kotlinw.di.api.Container
import xyz.kotlinw.di.api.PrecompiledScope
import xyz.kotlinw.di.api.Scope
import xyz.kotlinw.di.test.module1.Module1
import xyz.kotlinw.di.test.module2.Module2
import xyz.kotlinw.di.test.module3.Formatter
import xyz.kotlinw.di.test.module3.FormatterModule
import xyz.kotlinw.di.test.module3.GenericFormatter
import xyz.kotlinw.di.test.module4.Module4
import xyz.kotlinw.di.test.module5.Module5

@Container(Module1::class, Module2::class, FormatterModule::class, Module4::class, Module5::class)
interface SampleContainer {

    companion object

    @PrecompiledScope(modules = [Module1::class, Module4::class])
    fun rootScope(): Scope

//    @PrecompiledScope(modules = [Module5::class], parentScope = "rootScope")
//    fun nestedScope(parentScope: Scope): Scope

    @ComponentQuery
    fun getGenericFormatter(scope: Scope): GenericFormatter

    @ComponentQuery
    fun getFormatters(scope: Scope): List<Formatter<*>>

    @ComponentQuery
    fun getAny(scope: Scope): List<Any>
}
