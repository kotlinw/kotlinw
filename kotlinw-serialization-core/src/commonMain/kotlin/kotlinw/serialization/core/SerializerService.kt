package kotlinw.serialization.core

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.modules.SerializersModuleBuilder
import kotlinx.serialization.serializer

// TODO esetleg lehetne inkább ezt használni: val format = Json { serializersModule = projectModule + responseModule }
fun interface SerializersModuleContributor {

    context(SerializersModuleBuilder)
    fun configure()
}

@MustBeDocumented
@Retention(value = AnnotationRetention.BINARY)
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This is a delicate API and its use requires care." +
            " Make sure you fully read and understand documentation of the declaration that is marked as a delicate API."
)
annotation class DelicateSerializerServiceApi

interface SerializerService {

    companion object {

        @DelicateSerializerServiceApi
        val defaultSerializerService = SerializerServiceImpl()

        @OptIn(DelicateSerializerServiceApi::class)
        private val defaultSerializerServiceContext = object : HasSerializerService {
            override val serializerService get() = defaultSerializerService
        }

        @DelicateSerializerServiceApi
        fun <T> withDefaultSerializerService(block: HasSerializerService.() -> T): T =
            defaultSerializerServiceContext.block()
    }

    val json: Json

    fun <T> serialize(serializer: SerializationStrategy<T>, value: T) = json.encodeToString(serializer, value)

    fun <T> serializeToJsonElement(serializer: SerializationStrategy<T>, value: T) =
        json.encodeToJsonElement(serializer, value)

    fun <T> deserialize(
        deserializer: DeserializationStrategy<T>,
        serializedValue: String
    ): T = json.decodeFromString(deserializer, serializedValue)

    fun <T> deserializeFromJsonElement(
        deserializer: DeserializationStrategy<T>,
        serializedValue: JsonElement
    ): T = json.decodeFromJsonElement(deserializer, serializedValue)
}

inline fun <reified T> SerializerService.serialize(value: T): String = serialize(serializer(), value)

inline fun <reified T : Any> SerializerService.deserialize(serializedValue: String): T =
    deserialize(serializer(), serializedValue)

interface HasSerializerService {
    val serializerService: SerializerService
}
