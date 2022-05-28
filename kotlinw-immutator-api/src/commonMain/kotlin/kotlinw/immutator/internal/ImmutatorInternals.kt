package kotlinw.immutator.internal

import kotlinw.immutator.api.ConfinedMutableList
import kotlinw.immutator.api.ImmutableObject
import kotlinw.immutator.api.MutableObject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.LocalDate
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

interface MutableObjectImplementor<MutableType: MutableObject<ImmutableType>, ImmutableType> : MutableObject<ImmutableType> {
    val _isModified: Boolean
}

data class MutableValuePropertyState(
    var currentValue: Any?
)

class MutableReferencePropertyState(initialValue: MutableObjectImplementor<*, *>?) {
    private var _currentValue: MutableObjectImplementor<*, *>? = initialValue

    private var _isModified: Boolean = false

    val isModified get() = _isModified || _currentValue?._isModified ?: false

    var currentValue
        get() = _currentValue
        set(value) {
            _currentValue = value
            _isModified = true
        }
}

data class MutableObjectState<MutableType, ImmutableType>(
    private val sourceObject: ImmutableType,
    private val properties: Map<String, KProperty1<ImmutableType, *>>
) {
    private val valuePropertyStateMap: MutableMap<String, MutableValuePropertyState> = HashMap()

    private val referenceProperties: MutableMap<String, MutableReferencePropertyState> = HashMap()

    val isModified: Boolean
        get() =
            valuePropertyStateMap.isNotEmpty() || referenceProperties.any { it.value.isModified }

    private fun getPropertyOriginalValue(propertyName: String) = properties.getValue(propertyName).get(sourceObject)

    fun <P> getValueProperty(propertyName: String): P =
        if (valuePropertyStateMap.containsKey(propertyName)) {
            valuePropertyStateMap[propertyName]?.currentValue as P
        } else {
            getPropertyOriginalValue(propertyName) as P
        }

    fun <P> setValueProperty(propertyName: String, value: P) {
        // TODO esetleg optimalizálni, ha az eredetire állítjuk vissza? (csak value property-k esetében egyszerű megoldani)
        // TODO ha collection, akkor azt speciálisan, persistent collection-ökkel kezelni
        valuePropertyStateMap[propertyName] = MutableValuePropertyState(value)
    }

    fun <MutablePropertyType : MutableObjectImplementor<MutablePropertyType, ImmutablePropertyType>, ImmutablePropertyType : ImmutableObject<MutablePropertyType>> registerReferenceProperty(
        propertyName: String,
        initialValue: MutablePropertyType?
    ) {
        referenceProperties[propertyName] = MutableReferencePropertyState(initialValue)
    }

    fun <MutablePropertyType> getReferenceProperty(propertyName: String): MutablePropertyType =
        referenceProperties.getValue(propertyName).currentValue as MutablePropertyType

    fun <PropertyMutableType : MutableObjectImplementor<PropertyMutableType, *>> setReferenceProperty(
        propertyName: String,
        value: PropertyMutableType?
    ) {
        referenceProperties.getValue(propertyName).currentValue = value
    }

    fun <ElementMutableType, ElementImmutableType> getListProperty(
        propertyName: String
    ): ConfinedMutableList<ElementMutableType, ElementImmutableType> =
        getReferenceProperty(propertyName)

    fun <ElementMutableType, ElementImmutableType> registerListProperty(
        propertyName: String,
        sourceValue: ImmutableList<ElementImmutableType>,
        elementToMutable: (ElementImmutableType) -> ElementMutableType,
        elementToImmutable: (ElementMutableType) -> ElementImmutableType
    ) {
        val confinedMutableList = ConfinedMutableListImpl(sourceValue, elementToMutable, elementToImmutable)
        referenceProperties[propertyName] = MutableReferencePropertyState(confinedMutableList)
    }
}

fun <DefinitionType, MutableType : DefinitionType, ImmutableType : DefinitionType, P> MutableType.mutableValueProperty(
    objectState: MutableObjectState<MutableType, ImmutableType>
): PropertyDelegateProvider<MutableType, ReadWriteProperty<MutableType, P>> =
    PropertyDelegateProvider<MutableType, ReadWriteProperty<MutableType, P>> { mutableType, kProperty ->
        object : ReadWriteProperty<MutableType, P> {
            override fun getValue(thisRef: MutableType, property: KProperty<*>): P =
                objectState.getValueProperty(property.name)

            override fun setValue(thisRef: MutableType, property: KProperty<*>, value: P) {
                objectState.setValueProperty(property.name, value)
            }
        }
    }

fun <
        HostMutableType,
        HostImmutableType,
        PropertyMutableType : MutableObjectImplementor<PropertyMutableType, PropertyImmutableType>,
        PropertyImmutableType : ImmutableObject<PropertyMutableType>
        >
        HostMutableType.mutableReferenceProperty(
    objectState: MutableObjectState<HostMutableType, HostImmutableType>,
    sourceValue: PropertyImmutableType
): PropertyDelegateProvider<HostMutableType, ReadWriteProperty<HostMutableType, PropertyMutableType>> =
    PropertyDelegateProvider<HostMutableType, ReadWriteProperty<HostMutableType, PropertyMutableType>> { mutableType, kProperty ->
        objectState.registerReferenceProperty(
            kProperty.name,
            sourceValue.toMutable()
        )

        object : ReadWriteProperty<HostMutableType, PropertyMutableType> {
            override fun getValue(thisRef: HostMutableType, property: KProperty<*>): PropertyMutableType =
                objectState.getReferenceProperty(property.name)

            override fun setValue(thisRef: HostMutableType, property: KProperty<*>, value: PropertyMutableType) {
                objectState.setReferenceProperty(property.name, value)
            }
        }
    }

fun <
        HostMutableType,
        HostImmutableType,
        PropertyMutableType : MutableObjectImplementor<PropertyMutableType, PropertyImmutableType>,
        PropertyImmutableType : ImmutableObject<PropertyMutableType>
        >
        HostMutableType.mutableNullableReferenceProperty(
    objectState: MutableObjectState<HostMutableType, HostImmutableType>,
    sourceValue: PropertyImmutableType?
): PropertyDelegateProvider<HostMutableType, ReadWriteProperty<HostMutableType, PropertyMutableType?>> =
    PropertyDelegateProvider<HostMutableType, ReadWriteProperty<HostMutableType, PropertyMutableType?>> { mutableType, kProperty ->
        objectState.registerReferenceProperty(
            kProperty.name,
            sourceValue?.toMutable()
        )

        object : ReadWriteProperty<HostMutableType, PropertyMutableType?> {
            override fun getValue(thisRef: HostMutableType, property: KProperty<*>): PropertyMutableType? =
                objectState.getReferenceProperty(property.name)

            override fun setValue(thisRef: HostMutableType, property: KProperty<*>, value: PropertyMutableType?) {
                objectState.setReferenceProperty(property.name, value)
            }
        }
    }

fun <
        MutableType : MutableObject<ImmutableType>,
        ImmutableType : ImmutableObject<MutableType>,
        ElementMutableType : MutableObject<ElementImmutableType>,
        ElementImmutableType : ImmutableObject<ElementMutableType>,
        PropertyType : ConfinedMutableList<ElementMutableType, ElementImmutableType>
        >
        MutableType.mutableListProperty(
    objectState: MutableObjectState<MutableType, ImmutableType>,
    sourceValue: ImmutableList<ElementImmutableType>
): PropertyDelegateProvider<MutableType, ReadOnlyProperty<MutableType, PropertyType>> =
    PropertyDelegateProvider { _, kProperty ->

        objectState.registerListProperty(
            kProperty.name,
            sourceValue,
            { it.toMutable() },
            { it.toImmutable() }
        )

        ReadOnlyProperty { _, property ->
            objectState.getListProperty<ElementMutableType, ElementImmutableType>(property.name) as PropertyType // TODO
        }
    }

fun <
        MutableType : MutableObject<*>,
        ElementType,
        PropertyType : ConfinedMutableList<ElementType, ElementType>
        >
        MutableType.mutableListOfImmutableElements(
    objectState: MutableObjectState<MutableType, *>,
    sourceValue: ImmutableList<ElementType>
): PropertyDelegateProvider<MutableType, ReadOnlyProperty<MutableType, PropertyType>> =
    PropertyDelegateProvider { _, kProperty ->

        objectState.registerListProperty(
            kProperty.name,
            sourceValue,
            { it },
            { it }
        )

        ReadOnlyProperty { _, property ->
            objectState.getListProperty<ElementType, ElementType>(property.name) as PropertyType // TODO
        }
    }
