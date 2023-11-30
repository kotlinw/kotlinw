package kotlinw.module.hibernate.tool

import kotlinw.hibernate.core.schemaexport.ExportedSchemaScriptType
import kotlinw.hibernate.core.schemaexport.HibernateSqlSchemaExporter
import kotlinw.hibernate.core.schemaexport.HibernateSqlSchemaExporterImpl
import kotlinw.module.hibernate.core.HibernateModule
import org.hibernate.boot.Metadata
import org.hibernate.boot.registry.StandardServiceRegistry
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.ComponentQuery
import xyz.kotlinw.di.api.ContainerScope
import xyz.kotlinw.di.api.Module
import xyz.kotlinw.di.api.runJvmApplication
import xyz.kotlinw.module.appbase.api.AppbaseJvmModule

interface HibernateSqlSchemaExporterScope: ContainerScope {

    @ComponentQuery
    fun hibernateSqlSchemaExporter(): HibernateSqlSchemaExporter
}

@Module(includeModules = [HibernateModule::class, AppbaseJvmModule::class])
class HibernateSqlSchemaExporterModule {

    @Component
    fun hibernateSqlSchemaExporter(
        standardServiceRegistry: StandardServiceRegistry,
        metadata: Metadata
    ): HibernateSqlSchemaExporter = HibernateSqlSchemaExporterImpl(standardServiceRegistry, metadata)
}

suspend fun <T : HibernateSqlSchemaExporterScope> exportSqlSchema(rootScopeFactory: () -> T) {
    runJvmApplication(rootScopeFactory) {
        hibernateSqlSchemaExporter().exportSchema(ExportedSchemaScriptType.Update)
    }
}
