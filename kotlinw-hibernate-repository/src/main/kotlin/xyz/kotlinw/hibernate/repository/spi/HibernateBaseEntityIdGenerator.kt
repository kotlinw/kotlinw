package xyz.kotlinw.hibernate.repository.spi

import java.lang.reflect.Member
import java.util.*
import org.hibernate.boot.model.relational.Database
import org.hibernate.boot.model.relational.SqlStringGenerationContext
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.generator.EventType
import org.hibernate.id.IdentifierGenerator
import org.hibernate.id.OptimizableGenerator
import org.hibernate.id.PersistentIdentifierGenerator
import org.hibernate.id.enhanced.DatabaseStructure
import org.hibernate.id.enhanced.Optimizer
import org.hibernate.id.enhanced.SequenceStyleGenerator
import org.hibernate.id.factory.spi.CustomIdGeneratorCreationContext
import org.hibernate.service.ServiceRegistry
import org.hibernate.type.Type

private lateinit var sharedGenerator: SequenceStyleGenerator

private lateinit var originalConfigurationAnnotation: HibernateBaseEntityId

private lateinit var originalEntityName: String

class HibernateBaseEntityIdGenerator(
    private val configurationAnnotation: HibernateBaseEntityId,
    annotatedMember: Member,
    private val context: CustomIdGeneratorCreationContext
) : SequenceStyleGenerator() {

    override fun configure(type: Type, parameters: Properties, serviceRegistry: ServiceRegistry) {
        if (::sharedGenerator.isInitialized) {
            if (configurationAnnotation != originalConfigurationAnnotation) {
                throw IllegalStateException("Only one @${HibernateBaseEntityId::class.simpleName} configuration is allowed but found different ones: $originalConfigurationAnnotation on entity '$originalEntityName', and $configurationAnnotation on entity '${context.rootClass.entityName}'.")
            }
        } else {
            with(parameters) {
                setProperty(OptimizableGenerator.INITIAL_PARAM, configurationAnnotation.startWith.toString())
                setProperty(OptimizableGenerator.INCREMENT_PARAM, configurationAnnotation.incrementBy.toString())
                setProperty(IdentifierGenerator.ENTITY_NAME, HibernateBaseEntity::class.qualifiedName)
                setProperty(IdentifierGenerator.JPA_ENTITY_NAME, HibernateBaseEntity::class.qualifiedName)
                setProperty(SequenceStyleGenerator.SEQUENCE_PARAM, configurationAnnotation.sequenceName)
                remove(PersistentIdentifierGenerator.TABLES)
                remove(OptimizableGenerator.IMPLICIT_NAME_BASE)
                remove(PersistentIdentifierGenerator.TABLE)
            }

            originalConfigurationAnnotation = configurationAnnotation
            originalEntityName = context.rootClass.entityName

            sharedGenerator = SequenceStyleGenerator()
            sharedGenerator.configure(type, parameters, serviceRegistry)
        }
    }

    override fun generatesOnUpdate(): Boolean = sharedGenerator.generatesOnUpdate()

    override fun generatedOnExecution(): Boolean = sharedGenerator.generatedOnExecution()

    override fun generatedOnExecution(entity: Any?, session: SharedSessionContractImplementor?): Boolean =
        sharedGenerator.generatedOnExecution(entity, session)

    override fun allowAssignedIdentifiers(): Boolean = sharedGenerator.allowAssignedIdentifiers()

    override fun generatesSometimes(): Boolean = sharedGenerator.generatesSometimes()

    override fun generatesOnInsert(): Boolean = sharedGenerator.generatesOnInsert()

    override fun getEventTypes(): EnumSet<EventType> = sharedGenerator.getEventTypes()

    override fun generate(
        session: SharedSessionContractImplementor?,
        owner: Any?,
        currentValue: Any?,
        eventType: EventType?
    ): Any = sharedGenerator.generate(session, owner, currentValue, eventType)

    override fun supportsJdbcBatchInserts(): Boolean = sharedGenerator.supportsJdbcBatchInserts()

    override fun generate(session: SharedSessionContractImplementor?, `object`: Any?): Any =
        sharedGenerator.generate(session, `object`)

    override fun registerExportables(database: Database?) = sharedGenerator.registerExportables(database)

    override fun initialize(context: SqlStringGenerationContext?) = sharedGenerator.initialize(context)

    override fun getOptimizer(): Optimizer = sharedGenerator.getOptimizer()

    override fun supportsBulkInsertionIdentifierGeneration(): Boolean =
        sharedGenerator.supportsBulkInsertionIdentifierGeneration()

    override fun determineBulkInsertionIdentifierGenerationSelectFragment(context: SqlStringGenerationContext?): String =
        sharedGenerator.determineBulkInsertionIdentifierGenerationSelectFragment(context)

    override fun getDatabaseStructure(): DatabaseStructure = sharedGenerator.getDatabaseStructure()

    override fun getIdentifierType(): Type = sharedGenerator.getIdentifierType()
}
