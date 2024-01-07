package kotlinw.serialization.core

import kotlinx.serialization.modules.SerializersModule
import xyz.kotlinw.serialization.json.standardLongTermJson

class SerializerServiceImpl(
    private val serializersModuleContributors: List<SerializersModuleContributor> = emptyList(),
    private val prettyPrintJson: Boolean = true
) : SerializerService {

    override val json = standardLongTermJson {
        prettyPrint = prettyPrintJson
        serializersModule = SerializersModule {
            serializersModuleContributors.forEach { it.configure(this) }
        }
    }
}
