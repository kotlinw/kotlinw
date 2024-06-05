package xyz.kotlinw.jpa.repository

import kotlin.reflect.KClass
import kotlinw.uuid.Uuid
import kotlinw.uuid.toJavaUuid
import xyz.kotlinw.jpa.api.JpaSessionContext

interface BaseEntityRepository<E : BaseEntity> : AbstractEntityRepository<E, BaseEntityId>

abstract class BaseEntityRepositoryImpl<E : BaseEntity>(entityClass: KClass<E>) :
    AbstractEntityRepositoryImpl<E, BaseEntityId>(entityClass),
    BaseEntityRepository<E>

interface BaseIdentityEntityRepository<E : BaseIdentityEntity> : BaseEntityRepository<E> {

    context(JpaSessionContext)
    fun findByUid(uid: Uuid): E?
}

abstract class BaseIdentityEntityRepositoryImpl<E : BaseIdentityEntity>(entityClass: KClass<E>) :
    BaseEntityRepositoryImpl<E>(entityClass),
    BaseIdentityEntityRepository<E> {

        context(JpaSessionContext)
        override fun findByUid(uid: Uuid): E? =
            queryEntitySingleOrNull("from $entityName where uid=?1", uid.toJavaUuid())
    }
