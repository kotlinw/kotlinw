package kotlinw.configuration.toml

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.parsers.TomlParser
import com.akuleshov7.ktoml.tree.nodes.TomlArrayOfTablesElement
import com.akuleshov7.ktoml.tree.nodes.TomlFile
import com.akuleshov7.ktoml.tree.nodes.TomlInlineTable
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValueArray
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValuePrimitive
import com.akuleshov7.ktoml.tree.nodes.TomlNode
import com.akuleshov7.ktoml.tree.nodes.TomlStubEmptyNode
import com.akuleshov7.ktoml.tree.nodes.TomlTable
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlArray
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlValue
import kotlinw.configuration.core.ConfigurationPropertyKey
import kotlinw.configuration.core.ConfigurationPropertyKeySegment
import kotlinw.configuration.core.ConfigurationPropertySource
import kotlinw.configuration.core.ConfigurationPropertyValue
import kotlinw.util.stdlib.Priority
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

class TomlConfigurationPropertySource(
    private val source: Sequence<String>,
    override val priority: Priority = Priority.Normal
) : ConfigurationPropertySource {

    override fun getPropertyValue(key: ConfigurationPropertyKey): Any? {
        TODO("Not yet implemented")
    }
}

internal fun readToml(tomlContents: String): Map<ConfigurationPropertyKey, ConfigurationPropertyValue> =
    buildMap {
        val tomlFile = TomlParser(TomlInputConfig()).parseString(tomlContents)

        fun processTomlNode(node: TomlNode, keyPrefixSegments: PersistentList<ConfigurationPropertyKeySegment>) {

            fun convertValue(value: TomlValue) = value.content

            fun convertKey(key: String) =
                ConfigurationPropertyKeySegment(
                    if ((key.startsWith("\"") && key.endsWith("\""))
                        || (key.startsWith("'") && key.endsWith("'"))
                    ) {
                        key.substring(1, key.lastIndex)
                    } else {
                        key
                    }
                )

            fun processKeyValuePrimitive(
                node: TomlKeyValuePrimitive,
                keyPrefixSegments: PersistentList<ConfigurationPropertyKeySegment>
            ) {
                this[ConfigurationPropertyKey(keyPrefixSegments.add(convertKey(node.key.toString())))] =
                    convertValue(node.value)
            }

            fun processTable(table: TomlTable, keyPrefixSegments: PersistentList<ConfigurationPropertyKeySegment>) {
                table.getAllChildTomlTables().forEach {
                    val tablePrefixSegments =
                        keyPrefixSegments.add(ConfigurationPropertyKeySegment(it.fullTableKey.toString()))
                    it.children.forEach {
                        processTomlNode(it, tablePrefixSegments)
                    }
                }
            }

            fun processTomlArray(
                node: TomlKeyValueArray,
                keyPrefixSegments: PersistentList<ConfigurationPropertyKeySegment>
            ) {
                val arrayPrefixSegments = keyPrefixSegments.add(convertKey(node.key.toString()))

                @Suppress("UNCHECKED_CAST")
                ((node.value as TomlArray).content as List<TomlValue>).forEachIndexed { index, value ->
                    this[ConfigurationPropertyKey(arrayPrefixSegments.add(convertKey(index.toString())))] =
                        convertValue(value)
                }
            }

            when (node) {
                is TomlArrayOfTablesElement -> TODO()
                is TomlFile -> node.children.forEach { processTomlNode(it, keyPrefixSegments) }
                is TomlInlineTable -> TODO()
                is TomlKeyValueArray -> processTomlArray(node, keyPrefixSegments)
                is TomlKeyValuePrimitive -> processKeyValuePrimitive(node, keyPrefixSegments)
                is TomlStubEmptyNode -> {}
                is TomlTable -> processTable(node, keyPrefixSegments)
            }
        }

        processTomlNode(tomlFile, persistentListOf())
    }
