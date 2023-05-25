package kotlinw.hibernate.core.schemaupgrade.simple

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "DatabaseSchemaVersionInfo")
class DatabaseSchemaVersionInfoEntity(

    @Id
    var currentSchemaVersion: String,

    @Column(nullable = false)
    var timestamp: Instant
)
