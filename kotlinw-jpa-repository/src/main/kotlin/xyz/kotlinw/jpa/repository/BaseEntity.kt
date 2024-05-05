package xyz.kotlinw.jpa.repository

import arrow.core.continuations.AtomicRef
import jakarta.persistence.Column
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.SequenceGenerator
import java.util.*
import kotlinw.ulid.Ulid
import kotlinw.ulid.toUlid
import kotlinw.ulid.toUuid
import kotlinw.uuid.asJavaUuid
import kotlinw.uuid.toUuid

typealias BaseEntityId = Long

@MappedSuperclass
abstract class SimpleBaseEntity( // TODO rename to BaseEntity

    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "BaseEntityGenerator"
    )
    @SequenceGenerator(
        name = "BaseEntityGenerator",
        sequenceName = "bseq", // TODO rename: entity_sequence
        allocationSize = 50
    )
    override var id: BaseEntityId? = null

) : AbstractEntity<BaseEntityId>()

@MappedSuperclass
abstract class BaseEntity( // TODO rename to BaseEntityWithIdentity

    @Column(unique = true, nullable = false, updatable = false)
    open var uid: UUID = generateNextEntityUlid().toUuid().asJavaUuid()

) : SimpleBaseEntity() {

    var ulid: Ulid
        get() = uid.toUuid().toUlid()
        set(value) {
            uid = value.toUuid().asJavaUuid()
        }

    final override fun equals(other: Any?) =
        if (this === other) {
            true
        } else if (other is BaseEntity && ulid == other.ulid) {
            assert(resolveEntityClass() == other.resolveEntityClass())
            true
        } else {
            false
        }

    final override fun hashCode() = ulid.hashCode()

    override fun toString(): String {
        return "${resolveEntityClass().simpleName}(id=$id, uid=$uid)"
    }
}

private val previousEntityUlid = AtomicRef(Ulid.randomUlid())

fun generateNextEntityUlid(): Ulid = previousEntityUlid.updateAndGet { Ulid.nextMonotonicUlid(it) }
