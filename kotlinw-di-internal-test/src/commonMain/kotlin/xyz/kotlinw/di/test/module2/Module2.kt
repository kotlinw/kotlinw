package xyz.kotlinw.di.test.module2

import xyz.kotlinw.di.api.ComponentScan
import xyz.kotlinw.di.api.Module
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.test.module3.Formatter
import xyz.kotlinw.di.test.module3.FormatterModule

@Module(includeModules = [FormatterModule::class])
@ComponentScan
class Module2
