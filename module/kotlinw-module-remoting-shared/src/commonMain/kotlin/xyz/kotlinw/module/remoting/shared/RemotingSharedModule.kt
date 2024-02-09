package xyz.kotlinw.module.remoting.shared

import kotlinw.remoting.core.codec.JsonMessageCodec
import kotlinw.remoting.core.codec.MessageCodec
import kotlinw.serialization.core.SerializerService
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.Module
import xyz.kotlinw.serialization.json.standardLongTermJson

@Module
class RemotingSharedModule {

    @Component
    fun messageCodec(serializerService: SerializerService): MessageCodec<*> = JsonMessageCodec(serializerService.json)
}
