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
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import kotlinw.immutator.annotation.Immutable
import kotlinw.immutator.annotation.Immutate
import kotlinw.immutator.api.ConfinedMutableList
import kotlinw.immutator.internal.ImmutableObject
import kotlinw.immutator.internal.MutableObject
import kotlinw.immutator.internal.MutableObjectImplementor
import kotlinw.immutator.internal.MutableObjectState
import kotlinw.immutator.util.ImmutableBox
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.collections.immutable.toPersistentHashMap
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KProperty1

class ImmutatorException(override val message: String, val ksNode: KSNode, override val cause: Throwable? = null) :
    RuntimeException(message, cause)

internal val annotationQualifiedName = Immutate::class.qualifiedName!!

class ImmutatorSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {

    companion object {

        private val annotationSimpleName = Immutate::class.simpleName!!

        private val annotationDisplayName = "@$annotationSimpleName"

        private val convertToImmutableMethodName = MutableObject<*>::_immutator_convertToImmutable.name

        private val convertToMutableFunctionName = ImmutableObject<*>::_immutator_convertToMutable.name

        private val isModifiedPropertyName = MutableObjectImplementor<*, *>::_immutator_isModified.name
    }

    private val abstractPropertiesCache = ConcurrentHashMap<KSClassDeclaration, List<KSPropertyDeclaration>>()

    private lateinit var knownSubclasses: ImmutableMap<String, List<KSClassDeclaration>>

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val annotatedSymbols = resolver
            .getSymbolsWithAnnotation(annotationQualifiedName)
            .toList()

        val validClassDeclarations =
            annotatedSymbols
                .filter {
                    if (it !is KSClassDeclaration || it.classKind != INTERFACE) {
                        logger.error("Only interface declarations are supported by ${annotationDisplayName}.", it)
                        false
                    } else {
                        true
                    }
                }

        @Suppress("UNCHECKED_CAST")
        val symbolsToProcess = validClassDeclarations
            .filter { it.validate() }
            .toList() as List<KSClassDeclaration>

        fun KSClassDeclaration.mapKey(): String = qualifiedName!!.asString()

        fun KSTypeReference.mapKey(): String = resolve().declaration.asClassDeclaration!!.mapKey()

        symbolsToProcess.forEach { classDeclaration ->
            classDeclaration.superTypes
                .filter { it.resolve().declaration.qualifiedName?.asString() != Any::class.qualifiedName!! }
                .forEach {
                    if (!symbolsToProcess.map { it.mapKey() }.contains(it.mapKey())) {
                        logger.error("Supertypes must be in the same module: ${it.resolve()}", classDeclaration)
                    }
                }
        }

        // TODO már itt legyenek az ellenőrzések a valid-okra
        // TODO kizárni a nem egy modulban levő subinterface-eket

        knownSubclasses = buildMap<String, List<KSClassDeclaration>> {
            symbolsToProcess.forEach { symbol ->
                symbol.superTypes.filter {
                    symbolsToProcess.map { it.mapKey() }.contains(it.mapKey())
                }.forEach {
                    val key = it.mapKey()
                    this[key] = (this[key] ?: emptyList()) + symbol
                }
            }
        }.toPersistentHashMap()

        symbolsToProcess.forEach { symbol ->
            try {
                processClassDeclaration(symbol)
            } catch (e: ImmutatorException) {
                logger.error(e.message, e.ksNode)
            } catch (e: Exception) {
                logger.error("Internal processing error: ${e.message}", symbol)
            }
        }

        return annotatedSymbols.filter { !it.validate() }.toList()
    }

    private fun KSClassDeclaration.getKnownSubclasses(): List<KSClassDeclaration> =
        if (this.qualifiedName != null) {
            knownSubclasses[this.qualifiedName!!.asString()] ?: emptyList()
        } else {
            emptyList()
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

        if (classDeclaration.typeParameters.isNotEmpty()) {
            throw ImmutatorException(
                "Type parameters are not supported in case of $annotationDisplayName annotated interfaces.",
                classDeclaration
            )
        }

        if (classDeclaration.abstractProperties.isEmpty() && classDeclaration.getKnownSubclasses().toList()
                .isEmpty()
        ) {
            throw ImmutatorException(
                "An interface annotated with $annotationDisplayName must have at least one declared abstract property or have sub-interfaces.",
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
                "Only val properties are allowed in $annotationDisplayName annotated interfaces.",
                classDeclaration
            )
        }

        // TODO
//        var hasInvalidPropertyType = false
//        classDeclaration.abstractProperties.forEach {
//            if (it.type.resolve().propertyType == PropertyType.Unsupported) {
//                hasInvalidPropertyType = true
//                logger.error("Invalid property of unsupported type.", it)
//            }
//        }
//
//        if (hasInvalidPropertyType) {
//            throw ImmutatorException(
//                "Only properties of supported immutable types are allowed in $annotationDisplayName annotated interfaces.",
//                classDeclaration
//            )
//        }

        val definitionInterfaceName = classDeclaration.toClassName()
        val generatedFile = FileSpec.builder(
            definitionInterfaceName.packageName,
            definitionInterfaceName.simpleName + "ImmutateImpl"
        )
            .addImport(
                "kotlinw.immutator.internal",
                "mutableValueProperty",
                "mutableReferenceProperty",
                "mutableNullableReferenceProperty",
                "mutableListProperty",
                "mutableListOfImmutableElements"
            )

        when (classDeclaration.classKind) {
            INTERFACE -> {
                generatedFile.generateExtensionMethods(classDeclaration)

                generatedFile.generateMutableInterface(classDeclaration)

                if (classDeclaration.getKnownSubclasses().toList().isEmpty()) {
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
                                        PropertyType.Unsupported -> throwInternalCompilerError(
                                            it.type.resolve()
                                                .toString() + ", " + it.type.resolve().declaration.qualifiedName?.asString(),
                                            it
                                        ) // TODO
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
                                        PropertyType.Unsupported -> throwInternalCompilerError(
                                            it.type.resolve()
                                                .toString() + ", " + it.type.resolve().declaration.qualifiedName?.asString(),
                                            it
                                        ) // TODO
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
        Unsupported(false), Value(false), Immutated(false), Set(true), List(true), Map(true)
    }

    private val KSType.propertyType: PropertyType
        get() =
            if (declaration.qualifiedName?.asString() == Set::class.qualifiedName) {
                PropertyType.Set
            } else if (declaration.qualifiedName?.asString() == List::class.qualifiedName) {
                PropertyType.List
            } else if (declaration.qualifiedName?.asString() == Map::class.qualifiedName) {
                PropertyType.Map
            } else if (declaration.isImmutated) {
                PropertyType.Immutated
            } else if (isImmutable) {
                PropertyType.Value
            } else {
                PropertyType.Unsupported
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
                                """
                                if ($propertyName != other.$propertyName) {
                                    return false 
                                }
                                """.trimIndent()
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
                .addAnnotation(ClassName("androidx.compose.runtime", "Immutable"))
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
                        val resolvedPropertyType = propertyDeclaration.type.resolve()
                        addProperty(
                            PropertySpec.builder(
                                propertyDeclaration.simpleName.asString(),
                                when (resolvedPropertyType.propertyType) {
                                    PropertyType.Value -> propertyType.toTypeName() // TODO ez egységesen legyen kezelve
                                    PropertyType.Immutated -> propertyType.mutableTypeName
                                    PropertyType.Set -> propertyType.mutableTypeName
                                    PropertyType.List -> propertyType.mutableTypeName
                                    PropertyType.Map -> propertyType.mutableTypeName
                                    PropertyType.Unsupported -> throwInternalCompilerError(
                                        resolvedPropertyType.toString() + ", " + resolvedPropertyType.declaration.qualifiedName?.asString(),
                                        propertyDeclaration
                                    )
                                },
                                OVERRIDE
                            )
                                .mutable(!resolvedPropertyType.propertyType.isCollection)
                                .build()
                        )
                    }
                }
                .build()
        )
    }

    private fun FileSpec.Builder.generateExtensionMethods(definitionInterfaceDeclaration: KSClassDeclaration) {
        addFunction(
            FunSpec.builder("toMutable")
                .receiver(definitionInterfaceDeclaration.toClassName())
                .returns(definitionInterfaceDeclaration.mutableInterfaceName)
                .addCode(
                    // TODO support more complex hierarchy of classes
                    """
                        return when (this) {
                            ${
                        if (definitionInterfaceDeclaration.getKnownSubclasses().toList().isNotEmpty()) {
                            definitionInterfaceDeclaration.getKnownSubclasses().joinToString("\n") {
                                """
                                is ${it.mutableInterfaceName} -> this
                                is ${it.immutableDataClassName} -> ${convertToMutableFunctionName}()
                                """
                            }
                        } else {
                            """
                            is ${definitionInterfaceDeclaration.mutableInterfaceName} -> this
                            is ${definitionInterfaceDeclaration.immutableDataClassName} -> ${convertToMutableFunctionName}()
                            """.trimIndent()
                        }
                    }
                        else -> throw IllegalStateException()
                        }
                        """.trimIndent()
                )
                .build()
        )

        addFunction(
            FunSpec.builder("toImmutable")
                .receiver(definitionInterfaceDeclaration.toClassName())
                .returns(definitionInterfaceDeclaration.immutableDataClassName)
                .addCode(
                    // TODO support more complex hierarchy of classes
                    """
                        return when (this) {
                            ${
                        if (definitionInterfaceDeclaration.getKnownSubclasses().toList().isNotEmpty()) {
                            definitionInterfaceDeclaration.getKnownSubclasses().joinToString("\n") {
                                """
                                is ${it.immutableDataClassName} -> this
                                is ${it.mutableInterfaceName} -> (this as ${it.mutableClassName}).${convertToImmutableMethodName}()
                                """
                            }
                        } else {
                            """
                            is ${definitionInterfaceDeclaration.immutableDataClassName} -> this
                            is ${definitionInterfaceDeclaration.mutableInterfaceName} -> (this as ${definitionInterfaceDeclaration.mutableClassName}).${convertToImmutableMethodName}()
                            """.trimIndent()
                        }
                    }
                        else -> throw IllegalStateException()
                        }
                        """.trimIndent()
                )
                .build()
        )
    }

    private fun throwInternalCompilerError(message: String, ksNode: KSNode): Nothing {
        throw ImmutatorException("Internal compiler error: $message", ksNode)
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
                    } else if (ksType.isImmutable) {
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
        java.time.LocalDate::class, java.time.LocalDateTime::class, ZonedDateTime::class, java.time.Instant::class, //
        UUID::class, //
        ImmutableList::class, ImmutableSet::class, ImmutableMap::class
    )
        .map { it.qualifiedName!! }
        .toImmutableSet()

@OptIn(KspExperimental::class)
internal val KSDeclaration.isImmutated
    get() = isAnnotationPresent(Immutate::class)

@OptIn(KspExperimental::class)
internal val KSDeclaration.isMarkedImmutable
    get() = isAnnotationPresent(Immutable::class)

private val supportedVariances = setOf(Variance.COVARIANT, Variance.INVARIANT).toImmutableSet()

private val supportedCollectionClassNames =
    listOf(List::class)
        .map { it.qualifiedName!! }
        .toImmutableSet()

internal val KSClassDeclaration.isEnumClass: Boolean
    get() = modifiers.contains(Modifier.ENUM)

internal val KSType.isImmutable: Boolean
    get() = (
            declaration.qualifiedName?.asString() !in listOf(List::class.qualifiedName!!) && (
                    declaration.qualifiedName?.asString() == ImmutableBox::class.qualifiedName ||
                            (declaration.asClassDeclaration?.isEnumClass ?: false) ||
                            declaration.isMarkedImmutable ||
                            (
                                    arguments.all {
                                        (it.variance in supportedVariances) && (it.type?.resolve()?.isImmutable
                                            ?: false)
                                    } &&
                                            (declaration.asClassDeclaration?.isImmutable ?: false)
                                    )
                    )
            )

private val KSClassDeclaration.isImmutable: Boolean
    get() {
        val qualifiedName = qualifiedName?.asString()
        return qualifiedName != null &&
                (knownImmutableClassNames.contains(qualifiedName) ||
                        superTypes.map { it.resolve().declaration.qualifiedName?.asString() }
                            .contains(ImmutableObject::class.qualifiedName) ||
                        isInferredImmutable)
    }

internal val KSClassDeclaration.isInferredImmutable: Boolean
    get() =
        getAllProperties()
            .all {
                !it.isMutable &&
                        // TODO!it.isDelegated() && // TODO az isDelegated() valamiért true az Ulid.value-nál
                        it.type.resolve().isImmutable
            }
// TODO
//                    &&
//                    (!declaration.modifiers.contains(SEALED) ||
//                            declaration.getKnownSubclasses().all { it.isImmutable }) &&
//                    declaration.superTypes.all {
//                        val superTypeDeclaration = it.resolve().declaration
//                        (superTypeDeclaration is KSClassDeclaration && superTypeDeclaration.classKind == INTERFACE) ||
//                                superTypeDeclaration.modifiers.contains(SEALED)
//                    }
