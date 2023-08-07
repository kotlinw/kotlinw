package kotlinw.hibernate.core.schemaupgrade.simple

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import kotlinw.hibernate.core.entity.BaseEntity
import kotlinw.hibernate.core.entity.pgTextType
import java.time.Instant

@Entity
@Table(name = "DatabaseSchemaVersionInfo")
class DatabaseSchemaVersionInfoEntity(

    @Column(nullable = false, columnDefinition = pgTextType)
    var currentSchemaVersion: String,

    @Column(nullable = false)
    var timestamp: Instant

) : BaseEntity() {

    override fun toString(): String {
        return "DatabaseSchemaVersionInfoEntity(currentSchemaVersion='$currentSchemaVersion', timestamp=$timestamp)"
    }
}
