package kotlinw.hibernate.core.entity

import kotlin.reflect.KClass
import java.io.Serializable

data class EntityReference<EntityType : AbstractEntity<IdType>, IdType : Serializable>(
    val id: IdType,
    val entityClass: KClass<EntityType>
)

typealias BaseEntityReference<EntityType> = EntityReference<Long, EntityType>

inline fun <reified EntityType : AbstractEntity<IdType>, IdType : Serializable> EntityType.entityReference() =
    EntityReference(
        id ?: throw IllegalStateException(), // TODO
        EntityType::class
    )
