package xyz.kotlinw.module.hibernate.reactive

import kotlinw.module.hibernate.core.HibernateModule
import xyz.kotlinw.di.api.ComponentScan
import xyz.kotlinw.di.api.Module

@Module(includeModules = [HibernateModule::class])
@ComponentScan
class HibernateReactiveModule
