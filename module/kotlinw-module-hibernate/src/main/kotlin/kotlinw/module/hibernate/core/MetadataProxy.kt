package kotlinw.module.hibernate.core

import kotlinx.atomicfu.atomic
import org.hibernate.SessionFactory
import org.hibernate.boot.Metadata
import org.hibernate.boot.SessionFactoryBuilder
import org.hibernate.boot.model.IdentifierGeneratorDefinition
import org.hibernate.boot.model.NamedEntityGraphDefinition
import org.hibernate.boot.model.TypeDefinition
import org.hibernate.boot.model.relational.Database
import org.hibernate.boot.query.NamedHqlQueryDefinition
import org.hibernate.boot.query.NamedNativeQueryDefinition
import org.hibernate.boot.query.NamedProcedureCallDefinition
import org.hibernate.boot.query.NamedResultSetMappingDescriptor
import org.hibernate.engine.spi.FilterDefinition
import org.hibernate.mapping.Collection
import org.hibernate.mapping.FetchProfile
import org.hibernate.mapping.PersistentClass
import org.hibernate.mapping.Table
import org.hibernate.query.sqm.function.SqmFunctionDescriptor
import org.hibernate.type.Type
import java.util.*
import java.util.function.Consumer

internal class MetadataProxy : Metadata {

    private val delegateHolder = atomic<Metadata?>(null)

    private val delegate
        get() = delegateHolder.value ?: throw IllegalStateException("Delegate of $this is not initialized yet.")

    fun initialize(delegate: Metadata) {
        check(delegateHolder.value == null)
        delegateHolder.value = delegate
    }

    override fun getIdentifierType(className: String): Type = delegate.getIdentifierType(className)

    override fun getIdentifierPropertyName(className: String): String = delegate.getIdentifierPropertyName(className)

    override fun getReferencedPropertyType(className: String, propertyName: String): Type =
        delegate.getReferencedPropertyType(className, propertyName)

    override fun getSessionFactoryBuilder(): SessionFactoryBuilder = delegate.sessionFactoryBuilder

    override fun buildSessionFactory(): SessionFactory = delegate.buildSessionFactory()

    override fun getUUID(): UUID = delegate.uuid

    override fun getDatabase(): Database = delegate.database

    override fun getEntityBindings(): MutableCollection<PersistentClass> = delegate.entityBindings

    override fun getEntityBinding(entityName: String?): PersistentClass = delegate.getEntityBinding(entityName)

    override fun getCollectionBindings(): MutableCollection<Collection> = delegate.collectionBindings

    override fun getCollectionBinding(role: String?): Collection = delegate.getCollectionBinding(role)

    override fun getImports(): MutableMap<String, String> = delegate.imports

    override fun getNamedHqlQueryMapping(name: String?): NamedHqlQueryDefinition =
        delegate.getNamedHqlQueryMapping(name)

    override fun visitNamedHqlQueryDefinitions(definitionConsumer: Consumer<NamedHqlQueryDefinition>?) =
        delegate.visitNamedHqlQueryDefinitions(definitionConsumer)

    override fun getNamedNativeQueryMapping(name: String?): NamedNativeQueryDefinition =
        delegate.getNamedNativeQueryMapping(name)

    override fun visitNamedNativeQueryDefinitions(definitionConsumer: Consumer<NamedNativeQueryDefinition>?) =
        delegate.visitNamedNativeQueryDefinitions(definitionConsumer)

    override fun getNamedProcedureCallMapping(name: String?): NamedProcedureCallDefinition =
        delegate.getNamedProcedureCallMapping(name)

    override fun visitNamedProcedureCallDefinition(definitionConsumer: Consumer<NamedProcedureCallDefinition>?) =
        delegate.visitNamedProcedureCallDefinition(definitionConsumer)

    override fun getResultSetMapping(name: String?): NamedResultSetMappingDescriptor =
        delegate.getResultSetMapping(name)

    override fun visitNamedResultSetMappingDefinition(definitionConsumer: Consumer<NamedResultSetMappingDescriptor>?) =
        delegate.visitNamedResultSetMappingDefinition(definitionConsumer)

    override fun getTypeDefinition(typeName: String?): TypeDefinition = delegate.getTypeDefinition(typeName)

    override fun getFilterDefinitions(): MutableMap<String, FilterDefinition> = delegate.filterDefinitions

    override fun getFilterDefinition(name: String?): FilterDefinition = delegate.getFilterDefinition(name)

    override fun getFetchProfile(name: String?): FetchProfile = delegate.getFetchProfile(name)

    override fun getFetchProfiles(): MutableCollection<FetchProfile> = delegate.fetchProfiles

    override fun getNamedEntityGraph(name: String?): NamedEntityGraphDefinition = delegate.getNamedEntityGraph(name)

    override fun getNamedEntityGraphs(): MutableMap<String, NamedEntityGraphDefinition> = delegate.namedEntityGraphs

    override fun getIdentifierGenerator(name: String?): IdentifierGeneratorDefinition =
        delegate.getIdentifierGenerator(name)

    override fun collectTableMappings(): MutableCollection<Table> = delegate.collectTableMappings()

    override fun getSqlFunctionMap(): MutableMap<String, SqmFunctionDescriptor> = delegate.sqlFunctionMap

    override fun getContributors(): MutableSet<String> = delegate.contributors
}
