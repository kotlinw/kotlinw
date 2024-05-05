package xyz.kotlinw.hibernate.repository.spi

import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import xyz.kotlinw.jpa.repository.AbstractEntity
import xyz.kotlinw.jpa.repository.BaseEntity
import xyz.kotlinw.jpa.repository.BaseEntityId

@MappedSuperclass
class HibernateBaseEntity(

    @Id
    @HibernateBaseEntityId
    override var id: BaseEntityId? = null

) : AbstractEntity<BaseEntityId>()
