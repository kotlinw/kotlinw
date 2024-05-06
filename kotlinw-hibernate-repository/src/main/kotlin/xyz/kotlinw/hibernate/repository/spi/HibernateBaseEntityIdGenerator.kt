package xyz.kotlinw.hibernate.repository.spi

import java.lang.reflect.Member
import java.util.*
import org.hibernate.generator.EventType
import org.hibernate.generator.Generator
import org.hibernate.id.IdentifierGenerator
import org.hibernate.id.OptimizableGenerator
import org.hibernate.id.PersistentIdentifierGenerator
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
) : SequenceStyleGenerator(), Generator by sharedGenerator {

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

    override fun generatedOnExecution(): Boolean = sharedGenerator.generatedOnExecution()

    override fun getEventTypes(): EnumSet<EventType> = sharedGenerator.getEventTypes()
}
