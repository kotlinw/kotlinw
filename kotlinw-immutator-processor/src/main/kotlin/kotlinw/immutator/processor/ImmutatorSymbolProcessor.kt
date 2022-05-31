@file:OptIn(KotlinPoetKspPreview::class, KspExperimental::class)

package kotlinw.immutator.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind.INTERFACE
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Modifier.SEALED
import com.google.devtools.ksp.symbol.Variance
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import kotlinw.immutator.api.Immutate
import kotlinw.immutator.api.ConfinedMutableList
import kotlinw.immutator.api.Immutable
import kotlinw.immutator.internal.ImmutableObject
import kotlinw.immutator.internal.MutableObject
import kotlinw.immutator.internal.MutableObjectImplementor
import kotlinw.immutator.internal.MutableObjectState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KProperty1

class ImmutatorException(override val message: String, val ksNode: KSNode, override val cause: Throwable? = null) :
    RuntimeException(message, cause)

@KotlinPoetKspPreview
class ImmutatorSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {
    private val annotationQualifiedName = Immutate::class.qualifiedName!!

    private val annotationSimpleName = Immutate::class.simpleName!!

    private val annotationDisplayName = "@$annotationSimpleName"

    private val abstractPropertiesCache = ConcurrentHashMap<KSClassDeclaration, List<KSPropertyDeclaration>>()

    private val convertToImmutableMethodName = MutableObject<*>::_immutator_convertToImmutable.name

    private val convertToMutableFunctionName = ImmutableObject<*>::_immutator_convertToMutable.name

    private val isModifiedPropertyName = MutableObjectImplementor<*, *>::_immutator_isModified.name

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver
            .getSymbolsWithAnnotation(annotationQualifiedName)
            .toList()

        val invalidSymbols = symbols.filter { !it.validate() }.toList()

        symbols
            .filter { it.validate() }
            .forEach { symbol ->
                try {
                    if (symbol is KSClassDeclaration) {
                        processClassDeclaration(symbol)
                    } else {
                        logger.error("Unsupported annotated symbol: $symbol", symbol)
                    }
                } catch (e: ImmutatorException) {
                    logger.error(e.message, e.ksNode)
                } catch (e: Exception) {
                    logger.error("Internal processing error: ${e.message}", symbol)
                }
            }

        return invalidSymbols
    }

    private val KSClassDeclaration.abstractProperties: List<KSPropertyDeclaration>
        get() =
            abstractPropertiesCache.computeIfAbsent(this) {
                it.getAllProperties().filter { it.isAbstract() }.toList()
            }

    private fun processClassDeclaration(classDeclaration: KSClassDeclaration) {
        if (classDeclaration.classKind != INTERFACE) {
            throw ImmutatorException(
                "Only interfaces are allowed to be annotated with $annotationDisplayName.",
                classDeclaration
            )
        }

        if (!classDeclaration.modifiers.contains(SEALED)) {
            throw ImmutatorException(
                "An interface annotated with $annotationDisplayName must be sealed.",
                classDeclaration
            )
        }

        if (classDeclaration.typeParameters.isNotEmpty()) {
            throw ImmutatorException(
                "Type parameters are not supported in case of $annotationDisplayName annotated interfaces.",
                classDeclaration
            )
        }

        if (classDeclaration.abstractProperties.isEmpty()) {
            throw ImmutatorException(
                "An interface annotated with $annotationDisplayName must have at least one declared abstract property.",
                classDeclaration
            )
        }

        var hasMutableProperty = false
        classDeclaration.abstractProperties.forEach {
            if (it.isMutable) {
                hasMutableProperty = true
                logger.error("Mutable property is not allowed.", it)
            }
        }

        if (hasMutableProperty) {
            throw ImmutatorException(
                "Only val properties are allowed in case of $annotationDisplayName annotated interfaces.",
                classDeclaration
            )
        }

        val definitionInterfaceName = classDeclaration.toClassName()
        val generatedFile = FileSpec.builder(
            definitionInterfaceName.packageName,
            definitionInterfaceName.simpleName + "ImmutateImpl"
        )
            .addImport("kotlinw.immutator.internal", "mutableValueProperty")
            .addImport("kotlinw.immutator.internal", "mutableReferenceProperty")
            .addImport("kotlinw.immutator.internal", "mutableNullableReferenceProperty")
            .addImport("kotlinw.immutator.internal", "mutableListProperty")
            .addImport("kotlinw.immutator.internal", "mutableListOfImmutableElements")

        when (classDeclaration.classKind) {
            INTERFACE -> {
                generatedFile.generateMutableInterface(classDeclaration)

                if (classDeclaration.getSealedSubclasses().toList().isEmpty()) {
                    generateImmutableDataClass(classDeclaration, generatedFile)
                    generateMutableClass(classDeclaration, generatedFile)
                } else {
                    generateImmutableInterface(classDeclaration, generatedFile)
                }
            }
            else -> {
                logger.error("Unsupported class type: $classDeclaration", classDeclaration)
            }
        }

        generatedFile.build().writeTo(codeGenerator, false)
    }

    private fun generateImmutableInterface(
        definitionInterfaceDeclaration: KSClassDeclaration,
        generatedFile: FileSpec.Builder
    ) {
        generatedFile.addType(
            TypeSpec.interfaceBuilder(definitionInterfaceDeclaration.immutableDataClassName)
                .addOriginatingKSFile(definitionInterfaceDeclaration.containingFile!!)
                .addSuperinterface(definitionInterfaceDeclaration.toClassName())
                .addSuperinterface(
                    ImmutableObject::class.asClassName()
                        .parameterizedBy(definitionInterfaceDeclaration.mutableInterfaceName)
                )
                .build()
        )
    }

    private fun generateMutableClass(
        definitionInterfaceDeclaration: KSClassDeclaration,
        generatedFile: FileSpec.Builder
    ) {
        val immutableDataClassName = definitionInterfaceDeclaration.immutableDataClassName

        generatedFile.addType(
            TypeSpec.classBuilder(definitionInterfaceDeclaration.mutableClassName)
                .addOriginatingKSFile(definitionInterfaceDeclaration.containingFile!!)
                .addType(
                    TypeSpec.companionObjectBuilder()
                        .addProperty(
                            PropertySpec.builder(
                                "properties",
                                Map::class.asClassName().parameterizedBy(
                                    String::class.asClassName(),
                                    KProperty1::class.asClassName()
                                        .parameterizedBy(immutableDataClassName, STAR)
                                )
                            )
                                .initializer(
                                    buildCodeBlock {
                                        addStatement(
                                            """
                                        listOf(
                                            ${
                                                definitionInterfaceDeclaration.abstractProperties.joinToString {
                                                    immutableDataClassName.simpleName + "::" + it.simpleName.asString()
                                                }
                                            }
                                        ).associateBy { it.name }
                                        """.trimIndent()
                                        )
                                    }
                                )
                                .build()
                        )
                        .build()
                )
                .addSuperinterface(definitionInterfaceDeclaration.mutableInterfaceName)
                .addProperty(
                    PropertySpec.builder("source", immutableDataClassName)
                        .addModifiers(PRIVATE)
                        .initializer("source")
                        .build()
                )
                .primaryConstructor(
                    FunSpec.constructorBuilder()
                        .addParameter("source", immutableDataClassName)
                        .build()
                )
                .addProperty(
                    PropertySpec
                        .builder(
                            "_objectState",
                            MutableObjectState::class.asClassName().parameterizedBy(
                                definitionInterfaceDeclaration.mutableInterfaceName,
                                immutableDataClassName
                            )
                        )
                        .addModifiers(PRIVATE)
                        .initializer(
                            """
                            ${MutableObjectState::class.simpleName}(
                                source,
                                ${definitionInterfaceDeclaration.mutableClassName}.properties
                            )
                            """.trimIndent()
                        )
                        .build()
                )
                .addProperty(
                    PropertySpec
                        .builder(isModifiedPropertyName, Boolean::class.asClassName())
                        .addModifiers(OVERRIDE)
                        .getter(
                            FunSpec.getterBuilder().addCode("return _objectState.isModified").build()
                        )
                        .build()
                )
                .addProperties(
                    definitionInterfaceDeclaration.abstractProperties
                        .map {
                            PropertySpec
                                .builder(
                                    it.simpleName.asString(),
                                    it.type.mutableTypeName,
                                    OVERRIDE
                                )
                                .mutable(!it.type.resolve().propertyType.isCollection)
                                .delegate(
                                    when (it.type.resolve().propertyType) {
                                        PropertyType.Value -> "mutableValueProperty(_objectState)"
                                        PropertyType.Immutated -> if (it.type.resolve().isMarkedNullable) "mutableNullableReferenceProperty(_objectState, source.${it.simpleName.asString()})" else "mutableReferenceProperty(_objectState, source.${it.simpleName.asString()})"
                                        PropertyType.Set -> TODO(definitionInterfaceDeclaration.toString())
                                        PropertyType.List ->
                                            if (it.type.resolve().arguments[0].type!!.resolve().declaration.isImmutated) {
                                                "mutableListProperty(_objectState, source.${it.simpleName.asString()})"
                                            } else {
                                                "mutableListOfImmutableElements(_objectState, source.${it.simpleName.asString()})"
                                            }
                                        PropertyType.Map -> "mutableMapProperty(_objectState, source.${it.simpleName.asString()}, ::${it.type.resolve().arguments[1].type!!.resolve().declaration.asClassDeclaration!!.mutableClassName.simpleName}, ${it.type.resolve().arguments[1].type!!.resolve().declaration.asClassDeclaration!!.mutableClassName.simpleName}::toImmutable)"
                                    }
                                )
                                .build()
                        }
                        .toList()
                )
                .addFunction(
                    FunSpec.builder(convertToImmutableMethodName)
                        .returns(immutableDataClassName)
                        .addModifiers(OVERRIDE)
                        .addCode(
                            """
                            return if ($isModifiedPropertyName) {
                             ${immutableDataClassName}(${
                                definitionInterfaceDeclaration.abstractProperties.joinToString {
                                    val propertyName = it.simpleName.asString()
                                    when (it.type.resolve().propertyType) {
                                        PropertyType.Value -> propertyName
                                        PropertyType.Immutated -> if (it.type.resolve().isMarkedNullable) "$propertyName?.${convertToImmutableMethodName}()" else "$propertyName.${convertToImmutableMethodName}()"
                                        PropertyType.Set -> TODO()
                                        PropertyType.List -> if (it.type.resolve().isMarkedNullable) "$propertyName?.${convertToImmutableMethodName}()" else "$propertyName.${convertToImmutableMethodName}()"
                                        PropertyType.Map -> if (it.type.resolve().isMarkedNullable) "$propertyName?.${convertToImmutableMethodName}()" else "$propertyName.${convertToImmutableMethodName}()"
                                    }
                                }
                            })
                            } else {
                                source
                            }
                            """.trimIndent()
                        )
                        .build()
                )
                .generateEqualsAndHashCodeAndToString(definitionInterfaceDeclaration)
                .build()
        )
    }

    private enum class PropertyType(val isCollection: Boolean) {
        Value(false), Immutated(false), Set(true), List(true), Map(true)
    }

    private val KSType.propertyType: PropertyType
        get() =
            if (declaration.isImmutated) {
                PropertyType.Immutated
            } else if (isImmutable) {
                PropertyType.Value
            } else if (declaration.qualifiedName?.asString() == Set::class.qualifiedName) {
                PropertyType.Set
            } else if (declaration.qualifiedName?.asString() == ImmutableList::class.qualifiedName) {
                PropertyType.List
            } else if (declaration.qualifiedName?.asString() == List::class.qualifiedName) {
                PropertyType.List
            } else if (declaration.qualifiedName?.asString() == Map::class.qualifiedName) {
                PropertyType.Map
            } else {
                PropertyType.Immutated
            }

    private fun TypeSpec.Builder.generateEqualsAndHashCodeAndToString(definitionInterfaceDeclaration: KSClassDeclaration): TypeSpec.Builder {
        if (definitionInterfaceDeclaration.abstractProperties.isNotEmpty()) {
            addFunction(
                FunSpec.builder(Any::equals.name)
                    .returns(Boolean::class)
                    .addModifiers(OVERRIDE)
                    .addParameter("other", Any::class.asClassName().copy(nullable = true))
                    .addCode(
                        """
                    if (this === other) return true
                    if (other == null || other !is ${definitionInterfaceDeclaration.toClassName()}) {
                        return false 
                    }
                    
                    ${
                            definitionInterfaceDeclaration.abstractProperties.toList().joinToString("\n") {
                                val propertyName = it.simpleName.asString()
                                """if ($propertyName != other.$propertyName) { return false }"""
                            }
                        }

                    return true
                    """.trimIndent()
                    )
                    .build()
            )

            fun generateHashCodeExpression(it: KSPropertyDeclaration): String {
                val isNullable = it.type.resolve().isMarkedNullable
                return it.simpleName.asString() +
                        (if (isNullable) "?" else "") + ".hashCode()" +
                        (if (isNullable) " ?: 0" else "")
            }

            addFunction(
                FunSpec.builder(Any::hashCode.name)
                    .returns(Int::class)
                    .addModifiers(OVERRIDE)
                    .addCode(
                        definitionInterfaceDeclaration.abstractProperties
                            .mapIndexed { index, ksPropertyDeclaration ->
                                (if (index == 0) {
                                    "var result = " + generateHashCodeExpression(ksPropertyDeclaration)
                                } else {
                                    "result = 31 * result + (" + generateHashCodeExpression(ksPropertyDeclaration) + ")"
                                })
                            }
                            .joinToString(separator = "\n", postfix = "\nreturn result")
                    )
                    .build()
            )

            addFunction(
                FunSpec.builder(Any::toString.name)
                    .returns(String::class)
                    .addModifiers(OVERRIDE)
                    .addCode("return %P", definitionInterfaceDeclaration.simpleName.asString() + "(" + (
                            definitionInterfaceDeclaration.abstractProperties.joinToString {
                                val propertyName = it.simpleName.asString()
                                "$propertyName=\$$propertyName"
                            }
                            ) + ")"
                    )
                    .build()
            )
        }
        return this
    }

    private fun generateImmutableDataClass(
        definitionInterfaceDeclaration: KSClassDeclaration,
        generatedFile: FileSpec.Builder
    ) {
        generatedFile.addType(
            TypeSpec.classBuilder(definitionInterfaceDeclaration.immutableDataClassName)
                .addOriginatingKSFile(definitionInterfaceDeclaration.containingFile!!)
                .addSuperinterface(definitionInterfaceDeclaration.toClassName())
                .addModifiers(KModifier.DATA)
                .primaryConstructor(
                    FunSpec.constructorBuilder().addParameters(
                        definitionInterfaceDeclaration.abstractProperties
                            .map {
                                ParameterSpec.builder(
                                    it.simpleName.asString(),
                                    it.type.immutableTypeName
                                ).build()
                            }
                            .toList()
                    ).build()
                )
                .addProperties(
                    definitionInterfaceDeclaration.abstractProperties
                        .map {
                            PropertySpec.builder(
                                it.simpleName.asString(),
                                it.type.immutableTypeName,
                                OVERRIDE
                            )
                                .initializer(it.simpleName.asString())
                                .build()
                        }
                        .toList()
                )
                .addFunction(
                    FunSpec.builder(convertToMutableFunctionName)
                        .returns(definitionInterfaceDeclaration.mutableInterfaceName)
                        .addModifiers(OVERRIDE)
                        .addCode(
                            """
                        return ${definitionInterfaceDeclaration.mutableClassName}(this)
                        """.trimIndent()
                        )
                        .build()
                )
                .generateEqualsAndHashCodeAndToString(definitionInterfaceDeclaration)
                .apply {
                    val parentDefinitionInterface = definitionInterfaceDeclaration.parentDefinitionInterface
                    if (parentDefinitionInterface != null) {
                        addSuperinterface(parentDefinitionInterface.resolve().declaration.asClassDeclaration!!.immutableDataClassName)
                    } else {
                        addSuperinterface(
                            ImmutableObject::class.asClassName()
                                .parameterizedBy(definitionInterfaceDeclaration.mutableInterfaceName)
                        )
                    }
                }
                .build()
        )
    }

    private fun FileSpec.Builder.generateMutableInterface(definitionInterfaceDeclaration: KSClassDeclaration) {
        val definitionInterfaceClassName = definitionInterfaceDeclaration.toClassName()
        addType(
            TypeSpec.interfaceBuilder(definitionInterfaceDeclaration.mutableInterfaceName)
                .addOriginatingKSFile(definitionInterfaceDeclaration.containingFile!!)
                .addSuperinterface(definitionInterfaceClassName)
                .apply {
                    val parentDefinitionInterface = definitionInterfaceDeclaration.parentDefinitionInterface
                    if (parentDefinitionInterface != null) {
                        addSuperinterface(parentDefinitionInterface.resolve().declaration.asClassDeclaration!!.mutableInterfaceName)
                    } else {
                        addSuperinterface(
                            MutableObjectImplementor::class.asClassName()
                                .parameterizedBy(
                                    definitionInterfaceDeclaration.mutableInterfaceName,
                                    definitionInterfaceDeclaration.immutableDataClassName
                                )
                        )
                    }

                    definitionInterfaceDeclaration.abstractProperties.forEach { propertyDeclaration ->
                        val propertyType = propertyDeclaration.type
                        addProperty(
                            PropertySpec.builder(
                                propertyDeclaration.simpleName.asString(),
                                when (propertyDeclaration.type.resolve().propertyType) {
                                    PropertyType.Value -> propertyType.toTypeName() // TODO ez egységesen legyen kezelve
                                    PropertyType.Immutated -> propertyType.mutableTypeName
                                    PropertyType.Set -> propertyType.mutableTypeName
                                    PropertyType.List -> propertyType.mutableTypeName
                                    PropertyType.Map -> propertyType.mutableTypeName
                                },
                                OVERRIDE
                            )
                                .mutable(!propertyDeclaration.type.resolve().propertyType.isCollection)
                                .build()
                        )
                    }
                }
                .build()
        )
    }
}

internal val KSClassDeclaration.parentDefinitionInterface
    get() = superTypes.firstOrNull {
        it.resolve().declaration.isImmutated
    }

internal val KSTypeReference.mutableTypeName: TypeName
    get() {
        val ksType: KSType = resolve()
        val typeArguments: List<KSTypeArgument> = element?.typeArguments ?: emptyList()

        return with(ksType) {
            val declaration = ksType.declaration
            val resultType =
                if (declaration is KSClassDeclaration) {
                    if (declaration.isImmutated) {
                        declaration.mutableInterfaceName
                    } else if (declaration.isImmutable) {
                        this@mutableTypeName.toTypeName()
                    } else {
                        when (val typeName = ksType.toTypeName()) {
                            is ParameterizedTypeName ->
                                when (typeName.rawType) {
                                    ImmutableList::class.asClassName() -> ConfinedMutableList::class.asClassName()
                                        .parameterizedBy(
                                            typeArguments[0].type!!.mutableTypeName,
                                            typeArguments[0].type!!.immutableTypeName
                                        )
                                    List::class.asClassName() -> ConfinedMutableList::class.asClassName()
                                        .parameterizedBy(
                                            typeArguments[0].type!!.mutableTypeName,
                                            typeArguments[0].type!!.immutableTypeName
                                        )
                                    Set::class.asClassName() -> ClassName("kotlin.collections", "MutableSet")
                                        .parameterizedBy(typeArguments.map { it.type!!.mutableTypeName })
                                    Map::class.asClassName() -> ClassName("kotlin.collections", "MutableMap")
                                        .parameterizedBy(typeArguments.map { it.type!!.mutableTypeName })
                                    else -> throw UnsupportedOperationException(typeName.toString()) // TODO
                                }
                            else -> throw UnsupportedOperationException(typeName.toString()) // TODO
                        }
                    }
                } else {
                    throw UnsupportedOperationException()
                }

            if (ksType.isMarkedNullable) resultType.copy(nullable = true) else resultType
        }
    }

private val KSTypeReference.immutableTypeName: TypeName
    get() = getImmutableTypeName(resolve(), element?.typeArguments ?: emptyList())

private fun getImmutableTypeName(ksType: KSType, typeArguments: List<KSTypeArgument>): TypeName {
    val declaration = ksType.declaration
    val resultType = if (declaration is KSClassDeclaration) {
        if (declaration.isImmutated) {
            declaration.immutableDataClassName
        } else if (ksType.isImmutable) {
            val typeName = ksType.toTypeName()
            if (typeArguments.isEmpty()) {
                typeName
            } else {
                (typeName as? ClassName)?.parameterizedBy(typeArguments.map { it.type!!.mutableTypeName })
                    ?: typeName
            }
        } else {
            val typeName = ksType.toTypeName()
            if (declaration.modifiers.contains(Modifier.ENUM)) {
                typeName
            } else if (ksType.isImmutable) {
                if (typeArguments.isEmpty()) {
                    typeName
                } else {
                    (typeName as ClassName).parameterizedBy(typeArguments.map { it.type!!.mutableTypeName })
                }
            } else if (ksType.declaration.modifiers.contains(SEALED)) {
                typeName
            } else {
                when (typeName) {
                    is ParameterizedTypeName ->
                        when (typeName.rawType) {
                            List::class.asClassName() -> ImmutableList::class.asClassName()
                                .parameterizedBy(typeArguments.map { it.type!!.immutableTypeName }) // TODO !!
                            Set::class.asClassName() -> ImmutableSet::class.asClassName()
                                .parameterizedBy(typeArguments.map { it.type!!.immutableTypeName }) // TODO !!
                            Map::class.asClassName() -> ImmutableMap::class.asClassName()
                                .parameterizedBy(typeArguments.map { it.type!!.immutableTypeName }) // TODO !!
                            else -> throw UnsupportedOperationException(typeName.toString()) // TODO
                        }
                    else -> throw UnsupportedOperationException(typeName.toString()) // TODO
                }
            }
        }
    } else {
        throw UnsupportedOperationException() // TODO
    }

    return if (ksType.isMarkedNullable) resultType.copy(nullable = true) else resultType
}

private val KSClassDeclaration.immutableDataClassName: ClassName
    get() =
        toClassName().let {
            ClassName(
                it.packageName,
                it.simpleNames.dropLast(1) + (it.simpleNames.last() + "Immutable")
            )
        }

private val KSClassDeclaration.mutableInterfaceName: ClassName
    get() =
        toClassName().let {
            ClassName(
                it.packageName,
                it.simpleNames.dropLast(1) + (it.simpleNames.last() + "Mutable")
            )
        }

val KSClassDeclaration.mutableClassName: ClassName
    get() =
        toClassName().let {
            ClassName(
                it.packageName,
                it.simpleNames.dropLast(1) + (it.simpleNames.last() + "MutableImpl")
            )
        }

private val KSDeclaration.asClassDeclaration: KSClassDeclaration?
    get() =
        when (this) {
            is KSClassDeclaration -> this
            is KSTypeAlias -> type.resolve().declaration.asClassDeclaration
            else -> null
        }

private val knownImmutableClassNames =
    listOf(
        Boolean::class, Char::class, Byte::class, Short::class, Int::class, Long::class, Float::class, Double::class, //
        Unit::class, String::class, //
        LocalDate::class, LocalDateTime::class, Instant::class, //
        java.time.LocalDate::class, java.time.LocalDateTime::class, ZonedDateTime::class, java.time.Instant::class //
    )
        .map { it.qualifiedName!! }
        .toImmutableSet()

internal val KSDeclaration.isImmutated
    get() = isAnnotationPresent(Immutate::class)

internal val KSDeclaration.isMarkedImmutable
    get() = isAnnotationPresent(Immutable::class)

private val supportedVariances = setOf(Variance.COVARIANT, Variance.INVARIANT).toImmutableSet()

private val supportedImmutableCollectionClassNames =
    setOf(ImmutableList::class, ImmutableMap::class)
        .map { it.qualifiedName!! }
        .toImmutableSet()

internal val KSClassDeclaration.isEnumClass: Boolean
    get() = modifiers.contains(Modifier.ENUM)

internal val KSType.isImmutable: Boolean
    get() =
        arguments.all {
            (it.variance in supportedVariances) && (it.type?.resolve()?.isImmutable ?: false)
        } &&
                (declaration.asClassDeclaration?.isImmutable ?: false)

private val KSClassDeclaration.isImmutable: Boolean
    get() {
        val qualifiedName = qualifiedName?.asString()
        return qualifiedName != null &&
                (supportedImmutableCollectionClassNames.contains(qualifiedName) ||
                        knownImmutableClassNames.contains(qualifiedName) ||
                        isEnumClass ||
                        isMarkedImmutable ||
                        superTypes.map { it.resolve().declaration.qualifiedName?.asString() }
                            .contains(ImmutableObject::class.qualifiedName) ||
                        isInferredImmutable)
    }

internal val KSClassDeclaration.isInferredImmutable: Boolean
    get() =
        getAllProperties()
            .all { !it.isMutable && !it.isDelegated() && it.type.resolve().isImmutable }
// TODO
//                    &&
//                    (!declaration.modifiers.contains(SEALED) ||
//                            declaration.getSealedSubclasses().all { it.isImmutable }) &&
//                    declaration.superTypes.all {
//                        val superTypeDeclaration = it.resolve().declaration
//                        (superTypeDeclaration is KSClassDeclaration && superTypeDeclaration.classKind == INTERFACE) ||
//                                superTypeDeclaration.modifiers.contains(SEALED)
//                    }
