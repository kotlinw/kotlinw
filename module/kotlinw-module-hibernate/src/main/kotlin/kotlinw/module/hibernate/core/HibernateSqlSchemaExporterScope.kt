package kotlinw.module.hibernate.core

import kotlinw.hibernate.core.schemaexport.HibernateSqlSchemaExporter
import org.hibernate.SessionFactory
import xyz.kotlinw.di.api.ComponentQuery
import xyz.kotlinw.di.api.ContainerScope

interface HibernateSqlSchemaExporterScope: ContainerScope {

    @ComponentQuery
    fun hibernateSqlSchemaExporter(): HibernateSqlSchemaExporter
}
