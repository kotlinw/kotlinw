package kotlinw.serialization.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

class SerializerServiceImpl(
    private val serializersModuleContributors: List<SerializersModuleContributor> = emptyList(),
    private val prettyPrintJson: Boolean = true
) : SerializerService {

    override val json = Json(defaultSerializationJson) {
        prettyPrint = prettyPrintJson
        serializersModule = SerializersModule {
            serializersModuleContributors.forEach { it.configure(this) }
        }
    }
}
