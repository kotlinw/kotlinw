package kotlinw.module.hibernate.tool

import kotlinw.hibernate.core.schemaexport.HibernateSqlSchemaExporter
import kotlinw.koin.core.api.startContainer
import kotlinw.module.core.api.coreJvmModule
import org.koin.core.module.Module

suspend inline fun <reified T : Any> exportSqlSchema(jpaModule: Module) {
    val koinApplication = startContainer({
        modules(jpaModule, coreJvmModule<T>())
    })

    try {
        with(koinApplication.koin) {
            println(
                get<HibernateSqlSchemaExporter>().exportSchema(kotlinw.hibernate.core.schemaexport.ExportedSchemaScriptType.Update)
            )
        }
    } finally {
        koinApplication.close()
    }
}
