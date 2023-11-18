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
import kotlinw.configuration.core.EncodedConfigurationPropertyValue
import kotlinw.configuration.core.EnumerableConfigurationPropertyResolver
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

class TomlConfigurationPropertyResolver(
    tomlContents: String,
    sourceInfo: String? = null
) : EnumerableConfigurationPropertyResolver {

    private val properties = readToml(tomlContents)

    override suspend fun initialize() {}

    override fun getPropertyKeys() = properties.keys

    override fun getPropertyValueOrNull(key: ConfigurationPropertyKey): EncodedConfigurationPropertyValue? = properties[key]
}

internal fun readToml(tomlContents: String, sourceInfo: String? = null) =
    buildMap {
        val tomlFile = TomlParser(TomlInputConfig()).parseString(tomlContents)

        fun processTomlNode(node: TomlNode, keyPrefixSegments: PersistentList<ConfigurationPropertyKeySegment>) {

            fun convertValue(value: TomlValue): EncodedConfigurationPropertyValue = value.content.toString()

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

            fun createConfigurationPropertyKey(segments: PersistentList<ConfigurationPropertyKeySegment>, line: Int) =
                ConfigurationPropertyKey(segments, sourceInfo?.let { "$it@$line" })

            fun processKeyValuePrimitive(
                keyValuePrimitive: TomlKeyValuePrimitive,
                keyPrefixSegments: PersistentList<ConfigurationPropertyKeySegment>
            ) {
                this[
                    createConfigurationPropertyKey(
                        keyPrefixSegments.add(convertKey(keyValuePrimitive.key.toString())),
                        keyValuePrimitive.lineNo
                    )
                ] =
                    convertValue(keyValuePrimitive.value)
            }

            fun processTable(table: TomlTable, keyPrefixSegments: PersistentList<ConfigurationPropertyKeySegment>) {
                val tableKeyPrefixSegments = keyPrefixSegments.add(ConfigurationPropertyKeySegment(table.name))
                table.children.forEach {
                    processTomlNode(it, tableKeyPrefixSegments)
                }
            }

            fun processTomlArray(
                keyValueArray: TomlKeyValueArray,
                keyPrefixSegments: PersistentList<ConfigurationPropertyKeySegment>
            ) {
                val arrayPrefixSegments = keyPrefixSegments.add(convertKey(keyValueArray.key.toString()))

                @Suppress("UNCHECKED_CAST")
                ((keyValueArray.value as TomlArray).content as List<TomlValue>).forEachIndexed { index, value ->
                    this[
                        createConfigurationPropertyKey(
                            arrayPrefixSegments.add(convertKey(index.toString())),
                            keyValueArray.lineNo
                        )
                    ] =
                        convertValue(value)
                }
            }

            fun processFile(node: TomlFile, keyPrefixSegments: PersistentList<ConfigurationPropertyKeySegment>) {
                node.children.forEach {
                    processTomlNode(it, keyPrefixSegments)
                }
            }

            when (node) {
                is TomlArrayOfTablesElement -> TODO()
                is TomlFile -> processFile(node, keyPrefixSegments)
                is TomlInlineTable -> TODO()
                is TomlKeyValueArray -> processTomlArray(node, keyPrefixSegments)
                is TomlKeyValuePrimitive -> processKeyValuePrimitive(node, keyPrefixSegments)
                is TomlStubEmptyNode -> {}
                is TomlTable -> processTable(node, keyPrefixSegments)
            }
        }

        processTomlNode(tomlFile, persistentListOf())
    }
