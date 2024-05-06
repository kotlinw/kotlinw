package xyz.kotlinw.di.test

import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.ComponentQuery
import xyz.kotlinw.di.api.Container
import xyz.kotlinw.di.api.Scope
import xyz.kotlinw.di.api.ContainerScope
import xyz.kotlinw.di.test.module1.Module1
import xyz.kotlinw.di.test.module3.Formatter
import xyz.kotlinw.di.test.module3.GenericFormatter
import xyz.kotlinw.di.test.module4.Module4
import xyz.kotlinw.di.test.module5.Module5

object ExternalComponent

@Container
interface SampleContainer {

    companion object

    @Scope(modules = [Module1::class, Module4::class])
    fun rootScope(): RootScope

    @Scope(modules = [Module5::class], parent = "rootScope")
    fun nestedScope(parentScope: RootScope): NestedScope

    @Scope(modules = [Module5::class], parent = "rootScope")
    fun nestedScopeWithExternalComponent(
        parentScope: RootScope,
        @Component externalComponent: ExternalComponent
    ): NestedScope
}

interface RootScope : ContainerScope {

    @ComponentQuery
    fun getGenericFormatter(): GenericFormatter

    @ComponentQuery
    fun getFormatters(): List<Formatter<*>>

    @ComponentQuery
    fun getAny(): List<Any>

    @ComponentQuery
    fun getLongFormatter(): Formatter<Long>?
}

interface NestedScope : RootScope {
}
