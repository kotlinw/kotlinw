package kotlinw.hibernate.core.schemaexport

import org.hibernate.boot.Metadata
import org.hibernate.boot.registry.StandardServiceRegistry
import org.hibernate.tool.schema.TargetType
import org.hibernate.tool.schema.internal.DefaultSchemaFilter
import org.hibernate.tool.schema.internal.ExceptionHandlerHaltImpl
import org.hibernate.tool.schema.internal.exec.ScriptTargetOutputToWriter
import org.hibernate.tool.schema.spi.ContributableMatcher
import org.hibernate.tool.schema.spi.ExceptionHandler
import org.hibernate.tool.schema.spi.ExecutionOptions
import org.hibernate.tool.schema.spi.SchemaFilter
import org.hibernate.tool.schema.spi.SchemaManagementTool
import org.hibernate.tool.schema.spi.ScriptTargetOutput
import org.hibernate.tool.schema.spi.TargetDescriptor
import java.io.StringWriter
import java.util.EnumSet

enum class ExportedSchemaScriptType {
    Create, Update, Drop
}

interface HibernateSqlSchemaExporter {

    fun exportSchema(scriptType: ExportedSchemaScriptType): String
}

class HibernateSqlSchemaExporterImpl(
    private val standardServiceRegistry: StandardServiceRegistry,
    private val metadata: Metadata
) : HibernateSqlSchemaExporter {

    private data class ExecutionOptionsImpl(
        val _configurationValues: MutableMap<String, Any>,
        val _shouldManageNamespaces: Boolean,
        val _exceptionHandler: ExceptionHandler,
        val _schemaFilter: SchemaFilter
    ) : ExecutionOptions {

        override fun getConfigurationValues() = _configurationValues

        override fun shouldManageNamespaces() = _shouldManageNamespaces

        override fun getExceptionHandler() = _exceptionHandler

        override fun getSchemaFilter() = _schemaFilter
    }

    private data class TargetDescriptorImpl(
        val _targetTypes: EnumSet<TargetType>,
        val _scriptTargetOutput: ScriptTargetOutput
    ) : TargetDescriptor {

        override fun getTargetTypes() = _targetTypes

        override fun getScriptTargetOutput() = _scriptTargetOutput
    }

    override fun exportSchema(scriptType: ExportedSchemaScriptType): String {
        val schemaManagementTool = standardServiceRegistry.getService(SchemaManagementTool::class.java)

        val output = StringWriter()
        schemaManagementTool.getSchemaMigrator(emptyMap())
            .doMigration(
                metadata,
                ExecutionOptionsImpl(
                    mutableMapOf(),
                    false,
                    ExceptionHandlerHaltImpl.INSTANCE,
                    DefaultSchemaFilter.ALL
                ),
                ContributableMatcher.ALL,
                TargetDescriptorImpl(
                    EnumSet.of(TargetType.SCRIPT),
                    ScriptTargetOutputToWriter(output)
                )
            )

        return output.toString().trim().replace("\r\n", "\n")
    }
}
