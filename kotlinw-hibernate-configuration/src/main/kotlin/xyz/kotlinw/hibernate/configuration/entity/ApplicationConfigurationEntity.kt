package xyz.kotlinw.hibernate.configuration.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import kotlinw.hibernate.core.entity.BaseEntity
import kotlinw.hibernate.core.entity.BaseEntityRepository
import kotlinw.hibernate.core.entity.BaseEntityRepositoryImpl
import kotlinw.hibernate.core.entity.pgTextType
import org.hibernate.envers.Audited

@Entity
@Table(name = "ApplicationConfiguration")
@Audited
class ApplicationConfigurationEntity(

    @Column(nullable = false, columnDefinition = pgTextType)
    var name: String,

    @Column(nullable = false, columnDefinition = pgTextType)
    var value: String

) : BaseEntity() {

    override fun toString(): String {
        return "ApplicationConfigurationEntity(name='$name', value='$value')"
    }
}

interface ApplicationConfigurationEntityRepository : BaseEntityRepository<ApplicationConfigurationEntity>

class ApplicationConfigurationEntityRepositoryImpl :
    BaseEntityRepositoryImpl<ApplicationConfigurationEntity>(ApplicationConfigurationEntity::class),
    ApplicationConfigurationEntityRepository
