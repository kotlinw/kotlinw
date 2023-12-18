package xyz.kotlinw.module.serializer

import kotlinw.serialization.core.SerializerService
import kotlinw.serialization.core.SerializerServiceImpl
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.Module

@Module
class SerializerModule {

    @Component
    fun serializerService(): SerializerService = SerializerServiceImpl()
}