package kotlinw.uuid

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class ReferenceByUuid<T : HasUuid>(
    @Serializable(with = UuidSerializer::class)
    val uuid: Uuid
) {
    constructor(obj: T) : this(obj.uuid)

    override fun toString() = uuid.toString()
}
