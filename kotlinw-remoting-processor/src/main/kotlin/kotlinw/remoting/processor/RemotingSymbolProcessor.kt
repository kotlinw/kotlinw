package kotlinw.remoting.processor

import arrow.core.raise.Raise
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.ClassKind.ENUM_CLASS
import com.google.devtools.ksp.symbol.ClassKind.INTERFACE
import com.google.devtools.ksp.symbol.FunctionKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Variance
import com.google.devtools.ksp.symbol.impl.binary.KSFunctionDeclarationDescriptorImpl
import com.google.devtools.ksp.symbol.impl.kotlin.KSFunctionDeclarationImpl
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import com.squareup.kotlinpoet.typeNameOf
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlinw.ksp.util.filterAnnotations
import kotlinw.ksp.util.hasCompanionObject
import kotlinw.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.psi.KtContextReceiver
import org.jetbrains.kotlin.psi.KtProjectionKind
import org.jetbrains.kotlin.psi.KtProjectionKind.IN
import org.jetbrains.kotlin.psi.KtProjectionKind.NONE
import org.jetbrains.kotlin.psi.KtProjectionKind.OUT
import org.jetbrains.kotlin.psi.KtProjectionKind.STAR
import org.jetbrains.kotlin.psi.KtUserType
import xyz.kotlinw.remoting.api.RemotingClient
import xyz.kotlinw.remoting.api.SupportsRemoting
import xyz.kotlinw.remoting.api.internal.RemoteCallHandler
import xyz.kotlinw.remoting.api.internal.RemoteCallHandlerImplementor
import xyz.kotlinw.remoting.api.internal.RemotingClientCallSupport
import xyz.kotlinw.remoting.api.internal.RemotingClientFlowSupport
import xyz.kotlinw.remoting.api.internal.RemotingMethodDescriptor

// TODO ne generáljunk paraméter osztályt, ha 0 paramétere van a metódusnak
// TODO a @Serializable annotation-ök nem mennek át a metódus paraméterből a generált paraméter class property-jeibe
// TODO Raise<T> esetén T-nek is szerializálhatónak kell lenni
// TODO @Contextual-t is fogadjuk el isSerializable()-ben

private class StopKspProcessingException : RuntimeException()

class RemotingSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) :
    SymbolProcessor {

    companion object {

        private val ClassNameOfRaise = Raise::class.asClassName()
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols =
            resolver.getSymbolsWithAnnotation(SupportsRemoting::class.qualifiedName!!)
                .filterIsInstance<KSClassDeclaration>()
                .toSet()

        val validSymbols = symbols.filter { it.validate() }.toSet()
        val validSymbolsWithoutErrors = validSymbols.filter {
            with(resolver) {
                with(logger) {
                    isAnnotatedClassValid(it)
                }
            }
        }
        validSymbolsWithoutErrors.forEach {
            try {
                generateProxyClass(it, resolver)
            } catch (e: StopKspProcessingException) {
                // Ignore
            }
        }

        return (symbols - validSymbols).toList()
    }

    private fun generateProxyClass(definitionInterfaceDeclaration: KSClassDeclaration, resolver: Resolver) {
        val definitionInterfaceName = definitionInterfaceDeclaration.toClassName()

        val generatedFile = FileSpec.builder(
            definitionInterfaceName.packageName,
            definitionInterfaceDeclaration.toClassName().clientProxyClassName.simpleName
        )
        generateClientProxyClass(definitionInterfaceDeclaration, generatedFile, resolver)
        generatedFile.build().writeTo(codeGenerator, false)
    }

    private val ClassName.clientProxyClassName: ClassName
        get() = ClassName(packageName, simpleName + "ClientProxy")

    private val ClassName.remoteCallHandlerClassName: ClassName
        get() = ClassName(packageName, simpleName + "remoteCallHandler")

    private val KSTypeReference?.isUnit get() = this?.toTypeName() == Unit::class.asTypeName()

    @OptIn(ExperimentalKotlinPoetApi::class)
    private fun generateClientProxyClass(
        definitionInterfaceDeclaration: KSClassDeclaration,
        generatedFile: FileSpec.Builder,
        resolver: Resolver
    ) {
        val serializerFunctionName = MemberName("kotlinx.serialization", "serializer")

        val definitionInterfaceName = definitionInterfaceDeclaration.toClassName()
        val clientProxyClassName = definitionInterfaceName.clientProxyClassName

        val remotingClientParameterName = "remotingClient"
        val remotingClientCallSupportPropertyName = "remotingClientCallSupport"
        val remotingClientFlowSupportPropertyName = "remotingClientFlowSupport"
        val serviceIdPropertyName = "serviceId"

        val processedMemberFunctions = getProcessedMemberFunctions(definitionInterfaceDeclaration)

        fun KSFunctionDeclaration.parameterClassName(nestedClassClassifier: Int) =
            clientProxyClassName.nestedClass("Parameter_" + simpleName.asString() + "_cid" + nestedClassClassifier)

        fun KSFunctionDeclaration.resultClassName(nestedClassClassifier: Int) =
            clientProxyClassName.nestedClass("Result_" + simpleName.asString() + "_cid" + nestedClassClassifier)

        fun KSFunctionDeclaration.remotingMethodPath(nestedClassClassifier: Int) =
            simpleName.asString() + "_" + nestedClassClassifier // TODO customizable

        val serviceId = definitionInterfaceName.simpleName // TODO customizable

        fun buildResultClass(className: ClassName, normalResultType: TypeName, raisedErrors: List<TypeName>): TypeSpec {
            require(raisedErrors.isNotEmpty())
            val builder =
                TypeSpec.classBuilder(className)
                    .primaryConstructor(
                        FunSpec.constructorBuilder()
                            .addParameter(
                                ParameterSpec.builder("result", normalResultType.copy(nullable = true))
                                    .defaultValue("null").build()
                            )
                            .addParameter(
                                ParameterSpec.builder("errorIndex", typeNameOf<Int>().copy(nullable = true))
                                    .defaultValue("null").build()
                            )
                            .addParameters(
                                raisedErrors.mapIndexed { index, typeName ->
                                    ParameterSpec.builder("error$index", typeName.copy(nullable = true))
                                        .defaultValue("null").build()
                                }
                            )
                            .build()
                    )
                    .addProperty(
                        PropertySpec.builder("result", normalResultType.copy(nullable = true))
                            .initializer("result")
                            .build()
                    )
                    .addProperty(
                        PropertySpec.builder("errorIndex", typeNameOf<Int>().copy(nullable = true))
                            .initializer("errorIndex")
                            .build()
                    )
                    .addProperties(
                        raisedErrors.mapIndexed { index, typeName ->
                            PropertySpec.builder("error$index", typeName.copy(nullable = true))
                                .initializer("error$index")
                                .build()
                        }
                    )
                    .also {
                        it.addModifiers(KModifier.DATA)
                    }

            return builder
                .addModifiers(KModifier.INTERNAL) // TODO PRIVATE, but it makes unit testing impossible
                .addAnnotation(Serializable::class)
                .build()
        }

        fun KSFunctionDeclaration.raisedErrors() =
            with(resolver) {
                try {
                    when (this@raisedErrors) {
                        is KSFunctionDeclarationImpl -> ktFunction.contextReceivers
                        is KSFunctionDeclarationDescriptorImpl -> {
                            // TODO W-17
                            logger.warn("Context receivers are ignored (if any): ${this@raisedErrors}")
                            emptyList()
                        }
                        else -> throw IllegalStateException("Failed to extract context receivers: $this")
                    }
                        .map { it.toTypeName() }
                } catch (e: Exception) {
                    logger.error("Context receiver processing failed: $e", this@raisedErrors)
                    throw StopKspProcessingException()
                }
            }
                .also {
                    if (it.isNotEmpty()) {
                        logger.warn("Context receiver support is partial and highly experimental.")
                    }

                    if (it.any { !(it is ParameterizedTypeName && it.rawType == ClassNameOfRaise) }) {
                        logger.error("Only `$ClassNameOfRaise` is supported in context receiver.", this@raisedErrors)
                        throw StopKspProcessingException()
                    }
                }
                .map { it as ParameterizedTypeName }.map { it.typeArguments.first() }

        fun generateClientProxyClass(): TypeSpec {
            val builder = TypeSpec.classBuilder(clientProxyClassName)
                .addOriginatingKSFile(definitionInterfaceDeclaration.containingFile!!)
                .addModifiers(KModifier.PRIVATE)
                .addSuperinterface(definitionInterfaceName)
                .primaryConstructor(
                    FunSpec.constructorBuilder()
                        .addParameter(remotingClientParameterName, RemotingClient::class)
                        .build()
                )
                .addProperty(
                    PropertySpec.builder(remotingClientParameterName, RemotingClient::class)
                        .addModifiers(KModifier.PRIVATE)
                        .initializer(remotingClientParameterName)
                        .build()
                )
                .addProperty(
                    PropertySpec.builder(
                        remotingClientCallSupportPropertyName,
                        RemotingClientCallSupport::class
                    )
                        .addModifiers(KModifier.PRIVATE)
                        .getter(
                            FunSpec.getterBuilder().addStatement(
                                "return $remotingClientParameterName as %T",
                                RemotingClientCallSupport::class
                            ).build()
                        ) // TODO runtime hibaell.
                        .build()
                )
                .addProperty(
                    PropertySpec.builder(
                        remotingClientFlowSupportPropertyName,
                        RemotingClientFlowSupport::class
                    )
                        .addModifiers(KModifier.PRIVATE)
                        .getter(
                            FunSpec.getterBuilder().addStatement(
                                "return $remotingClientParameterName as %T",
                                RemotingClientFlowSupport::class
                            ).build()
                        ) // TODO runtime hibaell.
                        .build()
                )
                .addProperty(
                    PropertySpec.builder(serviceIdPropertyName, String::class)
                        .addModifiers(KModifier.PRIVATE)
                        .initializer("\"$serviceId\"")
                        .build()
                )

            processedMemberFunctions.forEachIndexed { nestedClassClassifier, function ->
                val functionName = function.simpleName.asString()
                val returnType = function.returnType?.resolve()
                val returnsFlow =
                    (returnType?.declaration as? KSClassDeclaration)?.toClassName() == Flow::class.asClassName()
                val parameters = function.parameters
                val hasParameters = parameters.isNotEmpty()
                val parameterClassName = function.parameterClassName(nestedClassClassifier)
                val raisedErrors = function.raisedErrors()

                builder.addType(
                    buildPayloadClass(
                        parameterClassName,
                        function.parameters
                    )
                ) // TODO ezt csináljuk meg előre, de a client proxy generálása közben

                val funBuilder = FunSpec.builder(functionName)
                    .addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
                    .contextReceivers(raisedErrors.map { Raise::class.asClassName().parameterizedBy(it) })
                    .addParameters(
                        function.parameters.map {
                            ParameterSpec.builder(it.name!!.asString(), it.type.toTypeName()).build()
                        }
                    )

                if (returnType != null) {
                    val resultClassName =
                        if (raisedErrors.isEmpty()) {
                            returnType.toTypeName()
                        } else {
                            val resultClassName = function.resultClassName(nestedClassClassifier)
                            builder.addType(
                                buildResultClass(
                                    resultClassName,
                                    returnType.toTypeName(),
                                    raisedErrors
                                )
                            ) // TODO ezt csináljuk meg előre, de a client proxy generálása közben
                            resultClassName
                        }

                    val methodPath = function.remotingMethodPath(nestedClassClassifier)

                    if (returnsFlow) {
                        funBuilder.addStatement(
                            """
                                return $remotingClientFlowSupportPropertyName.requestIncomingColdFlow(
                                    %T::class, 
                                    %T::%N,
                                    $serviceIdPropertyName,
                                    "$methodPath",
                                    %T${if (hasParameters) "(${parameters.joinToString { it.name!!.asString() }})" else ""},
                                    %M<%T>(),
                                    %M<%T>(),
                                    %M().toString()
                                )
                            """.trimIndent(),
                            definitionInterfaceName,
                            definitionInterfaceName,
                            functionName,
                            parameterClassName,
                            serializerFunctionName,
                            parameterClassName,
                            serializerFunctionName,
                            function.returnType?.element?.typeArguments?.get(0)?.toTypeName()
                                ?: Nothing::class.asClassName(),
                            MemberName(Uuid.Companion::class.asClassName(), "randomUuid")
                        )
                    } else {
                        funBuilder.addStatement(
                            """
                                ${if (returnType == typeNameOf<Unit>()) "" else "return·"}$remotingClientCallSupportPropertyName.call<%T, %T>(
                                    $serviceIdPropertyName,
                                    "$methodPath",
                                    %T${if (hasParameters) "(${parameters.joinToString { it.name!!.asString() }})" else ""},
                                    %M<%T>(),
                                    %M<%T>()
                                )
                                ${
                                if (raisedErrors.isNotEmpty()) {
                                    """
                                        .run {
                                            if (errorIndex == null) {
                                                result${if (returnType.isMarkedNullable) "" else "!!"}
                                            } else { 
                                                when (errorIndex) {
                                                     ${
                                        raisedErrors.mapIndexed { index: Int, typeName: TypeName -> "$index -> raise(error$index" + (if (typeName.isNullable) "" else "!!") + ")" }
                                            .joinToString("\n")
                                    }
                                                    else -> throw IllegalStateException()
                                                }
                                            }
                                        }
                                    """.trimIndent()
                                } else {
                                    ""
                                }
                            }
                            """.trimIndent(),
                            parameterClassName, resultClassName,
                            parameterClassName,
                            serializerFunctionName, parameterClassName,
                            serializerFunctionName, resultClassName
                        )
                    }

                    funBuilder.returns(returnType.toTypeName())
                    builder.addFunction(funBuilder.build())
                } else {
                    throw StopKspProcessingException()
                }
            }

            return builder.build()
        }

        fun generateServerDelegateClass(): TypeSpec {
            val builder = TypeSpec.classBuilder(definitionInterfaceName.remoteCallHandlerClassName)
                .addOriginatingKSFile(definitionInterfaceDeclaration.containingFile!!)
                .addModifiers(KModifier.PRIVATE)
                .addSuperinterface(
                    RemoteCallHandlerImplementor::class.asTypeName().parameterizedBy(definitionInterfaceName)
                )
                .primaryConstructor(
                    FunSpec.constructorBuilder()
                        .addParameter("target", definitionInterfaceName)
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("target", definitionInterfaceName)
                        .addModifiers(KModifier.PRIVATE)
                        .initializer("target")
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("serviceId", String::class)
                        .addModifiers(KModifier.OVERRIDE)
                        .initializer("\"$serviceId\"")
                        .build()
                )

            val methodDescriptorsBuilder =
                PropertySpec.builder(
                    "methodDescriptors",
                    typeNameOf<Map<String, RemotingMethodDescriptor>>(),
                    KModifier.OVERRIDE
                )
                    .initializer(
                        CodeBlock.builder().apply {
                            add("listOf(")
                            processedMemberFunctions.forEachIndexed { nestedClassIdentifier, function ->
                                val returnsFlow =
                                    (function.returnType?.resolve()?.declaration as? KSClassDeclaration)?.toClassName() == Flow::class.asClassName()
                                val raisedErrors = function.raisedErrors()

                                if (returnsFlow) {
                                    add(
                                        "%T(%S, %M<%T>(), %M<%T>()),",
                                        RemotingMethodDescriptor.DownstreamColdFlow::class,
                                        function.remotingMethodPath(nestedClassIdentifier),
                                        serializerFunctionName,
                                        function.parameterClassName(nestedClassIdentifier),
                                        serializerFunctionName,
                                        function.returnType?.element?.typeArguments?.get(0)?.toTypeName()
                                            ?: Nothing::class.asClassName()
                                    )
                                } else {
                                    add(
                                        "%T(%S, %M<%T>(), %M<%T>()),",
                                        RemotingMethodDescriptor.SynchronousCall::class,
                                        function.remotingMethodPath(nestedClassIdentifier),
                                        serializerFunctionName,
                                        function.parameterClassName(nestedClassIdentifier),
                                        serializerFunctionName,
                                        if (raisedErrors.isEmpty())
                                            function.returnType?.toTypeName()
                                        else
                                            function.resultClassName(nestedClassIdentifier)
                                    )
                                }
                            }
                            add(")\n.associateBy { it.memberId }")
                        }.build()
                    )
            builder.addProperty(methodDescriptorsBuilder.build())

            val processCallFunctionBuilder = FunSpec.builder("processCall")
                .addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
                .returns(typeNameOf<Any?>())
                .addParameter("methodId", String::class)
                .addParameter("parameter", Any::class)

            processCallFunctionBuilder.beginControlFlow("return when (methodId)")

            processedMemberFunctions.forEachIndexed { nestedClassIdentifier, function ->
                val functionName = function.simpleName.asString()
                val raisedErrors = function.raisedErrors()
                val returnType = function.returnType?.resolve()
                val resultClassName =
                    if (raisedErrors.isEmpty()) {
                        returnType?.toTypeName()
                    } else {
                        function.resultClassName(nestedClassIdentifier)
                    }

                processCallFunctionBuilder.beginControlFlow(
                    "%S ->",
                    function.remotingMethodPath(nestedClassIdentifier)
                )
                processCallFunctionBuilder.addStatement(
                    "val p = parameter as %T",
                    function.parameterClassName(nestedClassIdentifier)
                )

                fun buildTargetFunctionCall() =
                    "target.%N(${function.parameters.joinToString { "p." + it.name!!.asString() }})"

                if (raisedErrors.isEmpty()) {
                    processCallFunctionBuilder.addStatement(buildTargetFunctionCall(), functionName)
                } else {
                    processCallFunctionBuilder.addCode(
                        """
                        %M<%T, %T>({
                            ${buildTargetFunctionCall()}
                                .let { %T(it) }
                        }, {
                            when (it) {
                                ${
                            List(raisedErrors.size) { "is %T -> { %T(errorIndex = $it, error$it = it) }" }.joinToString(
                                "\n"
                            )
                        }
                                else -> throw IllegalStateException()
                            }
                        })
                        """.trimIndent(),
                        MemberName("arrow.core.raise", "recover"),
                        resolver.builtIns.anyType.makeNullable().toTypeName(),
                        resultClassName, // recover() type arguments
                        functionName,
                        resultClassName,
                        *(sequence { raisedErrors.map { yield(it); yield(resultClassName) } }.toList().toTypedArray())
                    )
                }

                processCallFunctionBuilder.endControlFlow()
            }

            processCallFunctionBuilder.beginControlFlow("else ->")
            processCallFunctionBuilder.addStatement("throw IllegalStateException(\"Invalid method ID: ${"$"}methodId\")")
            processCallFunctionBuilder.endControlFlow()
            processCallFunctionBuilder.endControlFlow()

            builder.addFunction(processCallFunctionBuilder.build())

            return builder.build()
        }

        generatedFile.addType(generateClientProxyClass())

        generatedFile.addFunction(
            FunSpec.builder("clientProxy")
                .receiver(definitionInterfaceName.nestedClass("Companion"))
                .addParameter("remotingClient", RemotingClient::class)
                .returns(definitionInterfaceName)
                .addStatement("return %T(remotingClient)", clientProxyClassName)
                .build()
        )

        generatedFile.addType(generateServerDelegateClass())

        generatedFile.addFunction(
            FunSpec.builder("remoteCallHandler")
                .receiver(definitionInterfaceName.nestedClass("Companion"))
                .addParameter("target", definitionInterfaceName)
                .returns(RemoteCallHandler::class.asTypeName().parameterizedBy(definitionInterfaceName))
                .addStatement("return %T(target)", definitionInterfaceName.remoteCallHandlerClassName)
                .build()
        )
    }

    private fun getProcessedMemberFunctions(definitionInterfaceDeclaration: KSClassDeclaration) =
        definitionInterfaceDeclaration.getAllFunctions().toList()
            .filter { it.functionKind == FunctionKind.MEMBER && it.isAbstract }

    private fun buildPayloadClass(className: ClassName, parameterTypes: List<KSValueParameter>): TypeSpec =
        buildPayloadClass(className, parameterTypes.associate { it.name!!.asString() to it.type })

    private fun buildPayloadClass(className: ClassName, parameterTypes: Map<String, KSTypeReference>): TypeSpec {
        val builder =
            if (parameterTypes.isEmpty()) {
                TypeSpec.objectBuilder(className)
            } else {
                TypeSpec.classBuilder(className)
                    .primaryConstructor(
                        FunSpec.constructorBuilder()
                            .addParameters(
                                parameterTypes.map {
                                    ParameterSpec(it.key, it.value.toTypeName())
                                }
                            )
                            .build()
                    )
                    .addProperties(
                        parameterTypes.map {
                            PropertySpec.builder(it.key, it.value.toTypeName())
                                .initializer(it.key)
                                .build()
                        }
                    )
                    .also {
                        if (parameterTypes.size == 1) {
                            it.addModifiers(KModifier.VALUE)
                            it.addAnnotation(JvmInline::class)
                        } else {
                            it.addModifiers(KModifier.DATA)
                        }
                    }
            }

        return builder
            .addModifiers(KModifier.INTERNAL) // TODO PRIVATE, but it makes unit testing impossible
            .addAnnotation(Serializable::class)
            .build()
    }

    context (Resolver, KSPLogger)
    private fun isAnnotatedClassValid(classDeclaration: KSClassDeclaration): Boolean {
        if (classDeclaration.classKind != ClassKind.INTERFACE) {
            logger.error(
                "Only interface declarations should be annotated with @${SupportsRemoting::class.simpleName}.",
                classDeclaration
            )
            return false
        }

        if (!classDeclaration.hasCompanionObject) {
            logger.error(
                "Interface declarations annotated with @${SupportsRemoting::class.simpleName} should have a `companion object`.",
                classDeclaration
            )
            return false
        }

        var hasMemberFunctionError = false
        getProcessedMemberFunctions(classDeclaration).forEach { memberFunctionDeclaration ->
            if (!memberFunctionDeclaration.modifiers.contains(Modifier.SUSPEND)) {
                logger.error("Member function should be 'suspend' to support remoting.", memberFunctionDeclaration)
                hasMemberFunctionError = true
            }

            if (memberFunctionDeclaration.extensionReceiver != null) {
                logger.error(
                    "Member function should not have extension receiver to support remoting.",
                    memberFunctionDeclaration
                )
                hasMemberFunctionError = true
            }

            val returnTypeReference = memberFunctionDeclaration.returnType
            if (returnTypeReference != null) {
                val returnType = returnTypeReference.resolve()
                if (returnType.declaration.qualifiedName?.asString() == Flow::class.qualifiedName!!) {
                    val flowValueTypeReference = returnType.arguments[0].type
                    if (flowValueTypeReference == null || !flowValueTypeReference.isSerializable("Flow value type")) {
                        hasMemberFunctionError = true
                    }
                } else if (!returnTypeReference.isSerializable("Return type")) {
                    hasMemberFunctionError = true
                }
            }

            memberFunctionDeclaration.parameters.forEach { parameter ->
                if (!parameter.type.isSerializable("Parameter ${parameter.name?.let { "'${it.asString()}' " } ?: ""}")) {
                    hasMemberFunctionError = true
                }
            }
        }

        return !hasMemberFunctionError
    }

}

context(Resolver, KSPLogger)
private fun KSTypeArgument.isSerializable(ksNodeDescription: String, depth: Int, maxDepth: AtomicInteger): Boolean {
    return type?.isSerializable(ksNodeDescription, depth + 1, maxDepth) ?: false
}

context(Resolver, KSPLogger)
private fun KSTypeReference.isSerializable(ksNodeDescription: String): Boolean {
    val maxDepth = AtomicInteger(0)
    return isSerializable(ksNodeDescription, 0, maxDepth)
}

context(Resolver, KSPLogger)
private fun KSTypeReference.isSerializable(ksNodeDescription: String, depth: Int, maxDepth: AtomicInteger): Boolean {
    maxDepth.set(max(depth, maxDepth.get()))

    val ksType = resolve().let { if (it is KSTypeAlias) it.type.resolve() else it }
    val notNullKsType = ksType.makeNotNullable()
    val ksDeclaration = notNullKsType.declaration
    val typeName = notNullKsType.toTypeName()
    val result =
        notNullKsType == builtIns.booleanType
                ||
                notNullKsType == builtIns.byteType
                ||
                notNullKsType == builtIns.shortType
                ||
                notNullKsType == builtIns.intType
                ||
                notNullKsType == builtIns.longType
                ||
                notNullKsType == builtIns.floatType
                ||
                notNullKsType == builtIns.doubleType
                ||
                notNullKsType == builtIns.charType
                ||
                notNullKsType == builtIns.stringType
                ||
                typeName == typeNameOf<BooleanArray>()
                ||
                typeName == typeNameOf<ByteArray>()
                ||
                typeName == typeNameOf<ShortArray>()
                ||
                typeName == typeNameOf<IntArray>()
                ||
                typeName == typeNameOf<LongArray>()
                ||
                typeName == typeNameOf<FloatArray>()
                ||
                typeName == typeNameOf<DoubleArray>()
                ||
                typeName == typeNameOf<CharArray>()
                ||
                (ksDeclaration is KSClassDeclaration && ksDeclaration.classKind == ENUM_CLASS)
                ||
                (ksDeclaration is KSClassDeclaration && ksDeclaration.classKind == INTERFACE)
                ||
                ksType.annotations.filterAnnotations<Serializable>().toList().isNotEmpty()
                ||
                (ksDeclaration == getClassDeclarationByName<Array<*>>()
                        && notNullKsType.arguments[0].isSerializable("Array element", depth, maxDepth))
                ||
                (ksDeclaration == getClassDeclarationByName<Pair<*, *>>()
                        && notNullKsType.arguments[0].isSerializable("Pair's first element", depth, maxDepth)
                        && notNullKsType.arguments[1].isSerializable("Pair's second element", depth, maxDepth)) ||
                (ksDeclaration == getClassDeclarationByName<Triple<*, *, *>>()
                        && notNullKsType.arguments[0].isSerializable("Triple's first element", depth, maxDepth)
                        && notNullKsType.arguments[1].isSerializable("Triple's second element", depth, maxDepth)
                        && notNullKsType.arguments[2].isSerializable("Triple's third element", depth, maxDepth))
                ||
                (ksDeclaration == getClassDeclarationByName<List<*>>()
                        && notNullKsType.arguments[0].isSerializable("List element", depth, maxDepth))
                ||
                (ksDeclaration == getClassDeclarationByName<Set<*>>()
                        && notNullKsType.arguments[0].isSerializable("Set element", depth, maxDepth))
                ||
                (ksDeclaration == getClassDeclarationByName<Map<*, *>>()
                        && notNullKsType.arguments[0].isSerializable("Map key", depth, maxDepth)
                        && notNullKsType.arguments[1].isSerializable("Map value", depth, maxDepth))
                ||
                (ksDeclaration as? KSClassDeclaration)?.classKind == ClassKind.OBJECT
                ||
                notNullKsType == builtIns.nothingType
                ||
                ksDeclaration.annotations.any { it.annotationType.resolve().declaration == getClassDeclarationByName<Serializable>() }

    if (!result && (depth == 0 || depth == maxDepth.get())) {
        error("$ksNodeDescription $ksType should be serializable to support remoting.")
    }

    return result
}

//
// Ugly hack to work with context receivers until official support is provided by KSP
//

private class ContextReceiverException(message: String) : RuntimeException(message)

context(Resolver)
private fun KtContextReceiver.toTypeName() = try {
    (typeReference()!!.typeElement as KtUserType).toKSType().toTypeName()
} catch (e: ContextReceiverException) {
    throw ContextReceiverException("Failed to resolve context receiver type: $text (${e.message})")
} catch (e: Exception) {
    throw ContextReceiverException("Failed to resolve context receiver type: $text")
}

private fun KtProjectionKind.toVariance() =
    when (this) {
        IN -> Variance.CONTRAVARIANT
        OUT -> Variance.COVARIANT
        STAR -> Variance.STAR
        NONE -> Variance.INVARIANT
    }

context(Resolver)
private fun KtUserType.toKSType(): KSType {
    val qualifiedName =
        (qualifier?.text
            ?: throw ContextReceiverException("Currently all type names must be fully qualified in the context receivers.")
                ) + "." + referencedName
    return getClassDeclarationByName(qualifiedName)
        ?.asType(
            typeArguments.map {
                getTypeArgument(
                    createKSTypeReferenceFromKSType(
                        (it.typeReference!!.typeElement as KtUserType).toKSType()
                    ),
                    it.projectionKind.toVariance()
                )
            }
        )
        ?: throw ContextReceiverException("Unknown type: $qualifiedName")
}
