package xyz.kotlinw.jpa.repository

import kotlin.reflect.KClass

interface SimpleBaseEntityRepository<E : SimpleBaseEntity> : AbstractEntityRepository<E, BaseEntityId>

abstract class SimpleBaseEntityRepositoryImpl<E : SimpleBaseEntity>(entityClass: KClass<E>) :
    AbstractEntityRepositoryImpl<E, BaseEntityId>(entityClass),
    SimpleBaseEntityRepository<E>

interface BaseIdentityEntityRepository<E : BaseIdentityEntity> : SimpleBaseEntityRepository<E>

abstract class BaseIdentityEntityRepositoryImpl<E : BaseIdentityEntity>(entityClass: KClass<E>) :
    SimpleBaseEntityRepositoryImpl<E>(entityClass),
    BaseIdentityEntityRepository<E>
