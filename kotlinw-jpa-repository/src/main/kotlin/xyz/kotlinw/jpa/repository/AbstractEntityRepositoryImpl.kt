package xyz.kotlinw.jpa.repository

import java.io.Serializable
import kotlin.reflect.KClass

abstract class AbstractEntityRepositoryImpl<E : AbstractEntity<ID>, ID : Serializable>(entityClass: KClass<E>) :
    GenericEntityRepositoryImpl<E, ID>(entityClass), AbstractEntityRepository<E, ID>
