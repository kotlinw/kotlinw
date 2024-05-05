package xyz.kotlinw.hibernate.configuration.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.Length
import org.hibernate.envers.Audited
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.jpa.api.JpaSessionContext
import xyz.kotlinw.jpa.repository.SimpleBaseEntity
import xyz.kotlinw.jpa.repository.SimpleBaseEntityRepository
import xyz.kotlinw.jpa.repository.SimpleBaseEntityRepositoryImpl

@Entity(name = ApplicationConfigurationEntity.TableName)
@Table
@Audited
class ApplicationConfigurationEntity(

    @Column(nullable = false, length = Length.LONG32, unique = true)
    var name: String,

    @Column(nullable = false, length = Length.LONG32)
    var value: String

) : SimpleBaseEntity() {

    companion object {

        const val TableName = "ApplicationConfiguration"
    }

    override fun toString(): String {
        return "ApplicationConfigurationEntity(name='$name', value='$value')"
    }
}

interface ApplicationConfigurationEntityRepository : SimpleBaseEntityRepository<ApplicationConfigurationEntity> {

    context(JpaSessionContext)
    fun findByName(name: String): ApplicationConfigurationEntity?
}

@Component
class ApplicationConfigurationEntityRepositoryImpl :
    SimpleBaseEntityRepositoryImpl<ApplicationConfigurationEntity>(ApplicationConfigurationEntity::class),
    ApplicationConfigurationEntityRepository {

    context(JpaSessionContext)
    override fun findByName(name: String): ApplicationConfigurationEntity? =
        querySingleOrNull("FROM $entityName WHERE name=?1", name)
}
