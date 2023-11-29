package kotlinw.module.hibernate.tool

import kotlinw.hibernate.core.schemaexport.ExportedSchemaScriptType
import kotlinw.hibernate.core.schemaexport.HibernateSqlSchemaExporter
import kotlinw.module.hibernate.core.HibernateSqlSchemaExporterScope
import xyz.kotlinw.di.api.ContainerScope
import xyz.kotlinw.di.api.runJvmApplication

suspend fun <T : HibernateSqlSchemaExporterScope> exportSqlSchema(rootScopeFactory: () -> T) {
    runJvmApplication(rootScopeFactory) {
        hibernateSqlSchemaExporter().exportSchema(ExportedSchemaScriptType.Update)
    }
}
