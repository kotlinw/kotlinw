package kotlinw.hibernate.core.entity

import arrow.core.continuations.AtomicRef
import jakarta.persistence.Column
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.SequenceGenerator
import kotlinw.ulid.Ulid
import kotlinw.ulid.toUlid
import kotlinw.ulid.toUuid
import kotlinw.uuid.asJavaUuid
import kotlinw.uuid.toUuid
import org.hibernate.Hibernate
import java.util.UUID

const val pgTextType = "TEXT" // TODO remove
const val pgUuidType = "UUID" // TODO remove

typealias BaseEntityId = Long

@MappedSuperclass
abstract class BaseEntity(

    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "BaseEntitySequenceGenerator"
    )
    @SequenceGenerator(
        name = "BaseEntitySequenceGenerator",
        sequenceName = "bseq",
        allocationSize = 50
    )
    override var id: BaseEntityId? = null,

    @Column(unique = true, nullable = false, updatable = false)
    open var uid: UUID = generateNextEntityUlid().toUuid().asJavaUuid()

) : AbstractEntity<BaseEntityId>() {

    var ulid: Ulid
        get() = uid.toUuid().toUlid()
        set(value) {
            uid = value.toUuid().asJavaUuid()
        }

    private inline val entityClassForEquals get(): Class<*> = Hibernate.getClass(this)

    final override fun equals(other: Any?) =
        if (this === other) {
            true
        } else if (other is BaseEntity && ulid == other.ulid) {
            assert(entityClassForEquals == other.entityClassForEquals)
            true
        } else {
            false
        }

    final override fun hashCode() = ulid.hashCode()

    override fun toString(): String {
        return "BaseEntity(id=$id, uid=$uid)"
    }
}

val previousEntityUlid = AtomicRef(Ulid.randomUlid())

fun generateNextEntityUlid(): Ulid = previousEntityUlid.updateAndGet { Ulid.nextMonotonicUlid(it) }

val BaseEntity.isSaved get() = id != null
