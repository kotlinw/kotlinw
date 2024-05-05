package kotlinw.hibernate.core.schemaupgrade.simple

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import org.hibernate.Length
import xyz.kotlinw.jpa.repository.AbstractEntity

@Entity
@Table(name = "DatabaseSchemaVersionInfo")
class DatabaseSchemaVersionInfoEntity(

    @Column(nullable = false, length = Length.LONG32)
    var currentSchemaVersion: String,

    @Column(nullable = false)
    var timestamp: Instant,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    override var id: Long? = null

) : AbstractEntity<Long>() {

    override fun toString(): String {
        return "DatabaseSchemaVersionInfoEntity(currentSchemaVersion='$currentSchemaVersion', timestamp=$timestamp)"
    }
}
