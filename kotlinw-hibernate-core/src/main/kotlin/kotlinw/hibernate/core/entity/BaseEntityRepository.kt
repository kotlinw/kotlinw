package kotlinw.hibernate.core.entity

import jakarta.persistence.LockModeType
import java.io.Serializable
import kotlin.reflect.KClass
import xyz.kotlinw.jpa.api.JpaSessionContext
import xyz.kotlinw.jpa.api.Transactional
import xyz.kotlinw.jpa.api.findOrNull
import xyz.kotlinw.jpa.api.getReference
import xyz.kotlinw.jpa.api.getSingleResultOrNull
import xyz.kotlinw.jpa.core.createTypedQuery
import xyz.kotlinw.jpa.core.executeQuery
import xyz.kotlinw.jpa.repository.AbstractEntity
import xyz.kotlinw.jpa.repository.AbstractEntityRepository
import xyz.kotlinw.jpa.repository.AbstractEntityRepositoryImpl

interface SimpleBaseEntityRepository<E : SimpleBaseEntity> : AbstractEntityRepository<E, BaseEntityId>

interface BaseEntityRepository<E : BaseEntity> : SimpleBaseEntityRepository<E>

abstract class SimpleBaseEntityRepositoryImpl<E : SimpleBaseEntity>(entityClass: KClass<E>) :
    AbstractEntityRepositoryImpl<E, BaseEntityId>(entityClass),
    SimpleBaseEntityRepository<E>

abstract class BaseEntityRepositoryImpl<E : BaseEntity>(entityClass: KClass<E>) :
    SimpleBaseEntityRepositoryImpl<E>(entityClass),
    BaseEntityRepository<E>
