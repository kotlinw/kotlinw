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
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import java.time.ZonedDateTime
import java.util.*
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

    private fun KSClassDeclaration.getKnownSubclasses(): List<KSClassDeclaration> = getSealedSubclasses().toList()

    private val KSClassDeclaration.abstractProperties: List<KSPropertyDeclaration>
        get() = getAllProperties().filter { it.isAbstract() }.toList()

    private fun processClassDeclaration(classDeclaration: KSClassDeclaration) {
        check(classDeclaration.classKind == INTERFACE)

        if (!classDeclaration.modifiers.contains(SEALED)) {
            throw ImmutatorException(
                "Interface annotated with $annotationDisplayName must be `sealed`.",
                classDeclaration
            )
        }

        if (!classDeclaration.hasCompanionObject) {
            throw ImmutatorException(
                "Interface annotated with $annotationDisplayName should have a `companion object`.",
                classDeclaration
            )
        }

        if (classDeclaration.typeParameters.isNotEmpty()) {
            throw ImmutatorException(
                "Type parameters are not supported in case of $annotationDisplayName annotated interfaces.",
                classDeclaration
            )
        }

        if (classDeclaration.abstractProperties.isEmpty()
            && classDeclaration.getKnownSubclasses().toList().isEmpty()
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
                logger.error(
                    "Mutable property is not allowed in an interface annotated with $annotationDisplayName.",
                    it
                )
            }
        }

        if (hasMutableProperty) {
            throw ImmutatorException(
                "Only `val` properties are allowed in $annotationDisplayName annotated interfaces.",
                classDeclaration
            )
        }

        var hasInvalidPropertyType = false
        classDeclaration.abstractProperties.forEach {
            if (it.type.resolve().typeCategory is TypeCategory.Unsupported) {
                hasInvalidPropertyType = true
                logger.error("Property has type that is not supported by $annotationDisplayName.", it)
            }
        }

        if (hasInvalidPropertyType) {
            throw ImmutatorException(
                "Only properties of supported types are allowed in interfaces annotated with $annotationDisplayName.",
                classDeclaration
            )
        }

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

        generatedFile.generateExtensionMethods(classDeclaration)

        generatedFile.generateMutableInterface(classDeclaration)

        if (classDeclaration.getKnownSubclasses().toList().isEmpty()) {
            generateImmutableDataClass(classDeclaration, generatedFile)
            generateMutableClass(classDeclaration, generatedFile)
        } else {
            generateImmutableInterface(classDeclaration, generatedFile)
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
                                .mutable(it.type.resolve().typeCategory !is TypeCategory.Collection)
                                .delegate(
                                    when (val propertyKind = it.type.resolve().typeCategory) {
                                        is TypeCategory.Value -> "mutableValueProperty(_objectState)"
                                        is TypeCategory.Immutated -> if (it.type.resolve().isMarkedNullable) "mutableNullableReferenceProperty(_objectState, source.${it.simpleName.asString()})" else "mutableReferenceProperty(_objectState, source.${it.simpleName.asString()})"
                                        is TypeCategory.Unsupported -> throwInternalCompilerError(
                                            it.type.resolve()
                                                .toString() + ", " + it.type.resolve().declaration.qualifiedName?.asString(),
                                            it
                                        ) // TODO
                                        is TypeCategory.Collection ->
                                            when (propertyKind.kind) {
                                                TypeCategory.Collection.Kind.Set -> TODO(definitionInterfaceDeclaration.toString())
                                                TypeCategory.Collection.Kind.List ->
                                                    if (it.type.resolve().arguments[0].type!!.resolve().declaration.isImmutated) {
                                                        "mutableListProperty(_objectState, source.${it.simpleName.asString()})"
                                                    } else {
                                                        "mutableListOfImmutableElements(_objectState, source.${it.simpleName.asString()})"
                                                    }

                                                TypeCategory.Collection.Kind.Map -> "mutableMapProperty(_objectState, source.${it.simpleName.asString()}, ::${it.type.resolve().arguments[1].type!!.resolve().declaration.asClassDeclarationOrNull()!!.mutableClassName.simpleName}, ${it.type.resolve().arguments[1].type!!.resolve().declaration.asClassDeclarationOrNull()!!.mutableClassName.simpleName}::toImmutable)"
                                            }
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
                                    when (val propertyKind = it.type.resolve().typeCategory) {
                                        is TypeCategory.Value -> propertyName
                                        is TypeCategory.Immutated -> if (it.type.resolve().isMarkedNullable) "$propertyName?.${convertToImmutableMethodName}()" else "$propertyName.${convertToImmutableMethodName}()"
                                        is TypeCategory.Unsupported -> throwInternalCompilerError(
                                            it.type.resolve()
                                                .toString() + ", " + it.type.resolve().declaration.qualifiedName?.asString(),
                                            it
                                        ) // TODO
                                        is TypeCategory.Collection ->
                                            when (propertyKind.kind) {
                                                TypeCategory.Collection.Kind.Set -> TODO()
                                                TypeCategory.Collection.Kind.List -> if (it.type.resolve().isMarkedNullable) "$propertyName?.${convertToImmutableMethodName}()" else "$propertyName.${convertToImmutableMethodName}()"
                                                TypeCategory.Collection.Kind.Map -> if (it.type.resolve().isMarkedNullable) "$propertyName?.${convertToImmutableMethodName}()" else "$propertyName.${convertToImmutableMethodName}()"
                                            }
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
                        addSuperinterface(parentDefinitionInterface.resolve().declaration.asClassDeclarationOrNull()!!.immutableDataClassName)
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
                        addSuperinterface(parentDefinitionInterface.resolve().declaration.asClassDeclarationOrNull()!!.mutableInterfaceName)
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
                                when (val propertyKind = resolvedPropertyType.typeCategory) {
                                    is TypeCategory.Value -> propertyType.toTypeName() // TODO ez egységesen legyen kezelve
                                    is TypeCategory.Immutated -> propertyType.mutableTypeName
                                    is TypeCategory.Unsupported -> throwInternalCompilerError(
                                        resolvedPropertyType.toString() + ", " + resolvedPropertyType.declaration.qualifiedName?.asString(),
                                        propertyDeclaration
                                    )

                                    is TypeCategory.Collection ->
                                        when (propertyKind.kind) {
                                            TypeCategory.Collection.Kind.Set -> propertyType.mutableTypeName
                                            TypeCategory.Collection.Kind.List -> propertyType.mutableTypeName
                                            TypeCategory.Collection.Kind.Map -> propertyType.mutableTypeName
                                        }
                                },
                                OVERRIDE
                            )
                                .mutable(resolvedPropertyType.typeCategory !is TypeCategory.Collection)
                                .build()
                        )
                    }
                }
                .build()
        )
    }

    private fun FileSpec.Builder.generateExtensionMethods(definitionInterfaceDeclaration: KSClassDeclaration) {
        if (definitionInterfaceDeclaration.getKnownSubclasses().isEmpty()) {
            addFunction(
                FunSpec.builder("immutable")
                    .receiver(definitionInterfaceDeclaration.companionObjectOrNull!!.toClassName())
                    .returns(definitionInterfaceDeclaration.immutableDataClassName)
                    .addParameters(
                        definitionInterfaceDeclaration.abstractProperties
                            .map {
                                ParameterSpec.builder(
                                    it.simpleName.asString(),
                                    it.type.immutableTypeName
                                ).build()
                            }
                            .toList()
                    )
                    .addStatement(
                        """
                        return %T(${definitionInterfaceDeclaration.abstractProperties.joinToString { "%N" }})
                    """.trimIndent(),
                        definitionInterfaceDeclaration.immutableDataClassName,
                        *definitionInterfaceDeclaration.abstractProperties.map { it.simpleName.asString() }
                            .toTypedArray()
                    )
                    .build()
            )
        }

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
                    } else if (ksType.isValueType) {
                        this@mutableTypeName.toTypeName()
                    } else {
                        when (val typeName = ksType.toClassName()) {
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
    val isNullable = ksType.isMarkedNullable
    val resultType = if (declaration is KSClassDeclaration) {
        if (declaration.isImmutated) {
            declaration.immutableDataClassName
        } else if (ksType.isValueType) {
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
            } else if (ksType.isValueType) {
                if (typeArguments.isEmpty()) {
                    typeName
                } else {
                    (typeName as ClassName).parameterizedBy(typeArguments.map { it.type!!.mutableTypeName })
                }
            } else if (ksType.declaration.modifiers.contains(SEALED)) {
                typeName
            } else {
                when (ksType.makeNotNullable().toClassName()) {
                    List::class.asClassName() -> ImmutableList::class.asClassName()
                        .parameterizedBy(typeArguments.map { it.type!!.immutableTypeName }) // TODO !!
                    Set::class.asClassName() -> ImmutableSet::class.asClassName()
                        .parameterizedBy(typeArguments.map { it.type!!.immutableTypeName }) // TODO !!
                    Map::class.asClassName() -> ImmutableMap::class.asClassName()
                        .parameterizedBy(typeArguments.map { it.type!!.immutableTypeName }) // TODO !!
                    else -> throw UnsupportedOperationException(typeName.toString()) // TODO
                }
                    .let { if (isNullable) it.copy(nullable = true) else it }
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

private fun KSDeclaration.asClassDeclarationOrNull(): KSClassDeclaration? =
    when (this) {
        is KSClassDeclaration -> this
        is KSTypeAlias -> type.resolve().declaration.asClassDeclarationOrNull()
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

internal val KSType.isValueType: Boolean
    get() {
        val declaration = this.declaration
        val qualifiedName = declaration.qualifiedName?.asString()
        return declaration is KSClassDeclaration
                && qualifiedName != null
                &&
                qualifiedName !in setOf(Set::class.qualifiedName, List::class.qualifiedName, Map::class.qualifiedName)
                && (
                knownImmutableClassNames.contains(qualifiedName)
                        || (
                        declaration.superTypes
                            .map { it.resolve().declaration.qualifiedName?.asString() }
                            .contains(ImmutableObject::class.qualifiedName)
                        )
                        || declaration.isInferredImmutable
                        || declaration.isMarkedImmutable
                        || declaration.isEnumClass
                )
    }

private sealed interface TypeCategory {

    val ksType: KSType

    data class Unsupported(override val ksType: KSType) : TypeCategory

    data class Value(override val ksType: KSType) : TypeCategory

    data class Immutated(override val ksType: KSType) : TypeCategory

    data class Collection(override val ksType: KSType, val kind: Kind) : TypeCategory {

        enum class Kind {
            Set, List, Map
        }
    }
}

private val KSType.typeCategory: TypeCategory
    get() =
        if (declaration.qualifiedName?.asString() == Set::class.qualifiedName) {
            TypeCategory.Collection(this, TypeCategory.Collection.Kind.Set)
        } else if (declaration.qualifiedName?.asString() == List::class.qualifiedName) {
            TypeCategory.Collection(this, TypeCategory.Collection.Kind.List)
        } else if (declaration.qualifiedName?.asString() == Map::class.qualifiedName) {
            TypeCategory.Collection(this, TypeCategory.Collection.Kind.Map)
        } else if (declaration.isImmutated) {
            TypeCategory.Immutated(this)
        } else if (isValueType) {
            TypeCategory.Value(this)
        } else {
            TypeCategory.Unsupported(this)
        }

internal val KSClassDeclaration.isInferredImmutable: Boolean
    get() =
        getAllProperties()
            .all {
                !it.isMutable &&
                        !it.isDelegated() &&
                        it.type.resolve().isValueType
            }
