package kotlinw.hibernate.core.schemaupgrade.simple

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.Instant
import org.hibernate.Length
import xyz.kotlinw.jpa.repository.BaseEntity

@Entity
@Table(name = "DatabaseSchemaVersionInfo")
class DatabaseSchemaVersionInfoEntity(

    @Column(nullable = false, length = Length.LONG32)
    var currentSchemaVersion: String,

    @Column(nullable = false)
    var timestamp: Instant,

    ) : BaseEntity() {

    override fun toString(): String {
        return "DatabaseSchemaVersionInfoEntity(currentSchemaVersion='$currentSchemaVersion', timestamp=$timestamp)"
    }
}
