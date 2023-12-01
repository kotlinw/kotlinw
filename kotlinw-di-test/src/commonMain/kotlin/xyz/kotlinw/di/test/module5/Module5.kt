package xyz.kotlinw.di.test.module5

import xyz.kotlinw.di.api.ComponentScan
import xyz.kotlinw.di.api.Module
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.test.ExternalComponent
import xyz.kotlinw.di.test.module2.Module2

@Module
@ComponentScan
class Module5 {

    @Component
    fun someComponent(externalComponent: ExternalComponent?) = object : SomeComponent {}
}

interface SomeComponent {

}
