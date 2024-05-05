package xyz.kotlinw.jpa.repository

import kotlin.reflect.KClass

interface BaseEntityRepository<E : BaseEntity> : AbstractEntityRepository<E, BaseEntityId>

abstract class BaseEntityRepositoryImpl<E : BaseEntity>(entityClass: KClass<E>) :
    AbstractEntityRepositoryImpl<E, BaseEntityId>(entityClass),
    BaseEntityRepository<E>

interface BaseIdentityEntityRepository<E : BaseIdentityEntity> : BaseEntityRepository<E>

abstract class BaseIdentityEntityRepositoryImpl<E : BaseIdentityEntity>(entityClass: KClass<E>) :
    BaseEntityRepositoryImpl<E>(entityClass),
    BaseIdentityEntityRepository<E>
