package kotlinw.common.util

import kotlinw.immutator.annotation.Immutable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Immutable
expect class Uuid {
    override fun toString(): String
}

expect object UuidSerializer : KSerializer<Uuid>

expect fun randomUuid(): Uuid

expect fun uuidFromString(uuidString: String): Uuid

interface HasUuid {
    val uuid: Uuid
}

@JvmInline
@Serializable
value class ReferenceByUuid<T : HasUuid>(
    @Serializable(with = UuidSerializer::class)
    val uuid: Uuid
) {
    constructor(obj: T) : this(obj.uuid)

    override fun toString() = uuid.toString()
}
