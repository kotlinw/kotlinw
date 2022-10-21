package kotlinw.uuid

import kotlinw.immutator.annotation.Immutable
import kotlinx.serialization.KSerializer

@Immutable
expect class Uuid {
    override fun toString(): String
}

expect object UuidSerializer : KSerializer<Uuid>

expect fun randomUuid(): Uuid

expect fun uuidFromString(uuidString: String): Uuid
