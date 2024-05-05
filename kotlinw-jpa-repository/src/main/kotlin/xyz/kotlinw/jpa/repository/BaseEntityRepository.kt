package xyz.kotlinw.jpa.repository

import kotlin.reflect.KClass

interface SimpleBaseEntityRepository<E : SimpleBaseEntity> : AbstractEntityRepository<E, BaseEntityId>

interface BaseEntityRepository<E : BaseEntity> : SimpleBaseEntityRepository<E>

abstract class SimpleBaseEntityRepositoryImpl<E : SimpleBaseEntity>(entityClass: KClass<E>) :
    AbstractEntityRepositoryImpl<E, BaseEntityId>(entityClass),
    SimpleBaseEntityRepository<E>

abstract class BaseEntityRepositoryImpl<E : BaseEntity>(entityClass: KClass<E>) :
    SimpleBaseEntityRepositoryImpl<E>(entityClass),
    BaseEntityRepository<E>
