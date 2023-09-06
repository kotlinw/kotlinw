package kotlinw.serialization.core

import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.bindSet
import org.kodein.di.instance
import org.kodein.di.singleton

object SerializationCoreKodeinModule {
    private const val moduleId = "kotlinw-serialization-core-common"

    val serializationCoreCommonKodeinModule by lazy {
        DI.Module(moduleId) {
            bindSet<SerializersModuleContributor>()

            bind<SerializerService> {
                singleton { SerializerServiceImpl(instance<Set<SerializersModuleContributor>>().toList()) } // TODO devmode-t átadni a konstuktorban
            }
        }
    }
}
