package xyz.kotlinw.hibernate.configuration.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import kotlinw.hibernate.core.api.JpaSessionContext
import kotlinw.hibernate.core.entity.BaseEntity
import kotlinw.hibernate.core.entity.BaseEntityRepository
import kotlinw.hibernate.core.entity.BaseEntityRepositoryImpl
import org.hibernate.Length
import org.hibernate.envers.Audited
import xyz.kotlinw.di.api.Component

@Entity
@Table(name = ApplicationConfigurationEntity.TableName)
@Audited
class ApplicationConfigurationEntity(

    @Column(nullable = false, length = Length.LONG32, unique = true)
    var name: String,

    @Column(nullable = false, length = Length.LONG32)
    var value: String

) : BaseEntity() {

    companion object {

        const val TableName = "ApplicationConfiguration"
    }

    override fun toString(): String {
        return "ApplicationConfigurationEntity(name='$name', value='$value')"
    }
}

interface ApplicationConfigurationEntityRepository : BaseEntityRepository<ApplicationConfigurationEntity> {

    context(JpaSessionContext)
    fun findByName(name: String): ApplicationConfigurationEntity?
}

@Component
class ApplicationConfigurationEntityRepositoryImpl :
    BaseEntityRepositoryImpl<ApplicationConfigurationEntity>(ApplicationConfigurationEntity::class),
    ApplicationConfigurationEntityRepository {

    context(JpaSessionContext)
    override fun findByName(name: String): ApplicationConfigurationEntity? =
        singleOrNull("FROM $entityName WHERE name=?1", name)
}
