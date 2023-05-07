package kotlinw.remoting.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.FunctionKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import com.squareup.kotlinpoet.typeNameOf
import kotlinw.remoting.api.SupportsRemoting
import kotlinw.remoting.api.client.RemotingClient
import kotlinw.remoting.api.internal.client.RemotingClientDownstreamFlowSupport
import kotlinw.remoting.api.internal.client.RemotingClientSynchronousCallSupport
import kotlinw.remoting.api.internal.server.RemoteCallDelegator
import kotlinw.remoting.api.internal.server.RemotingMethodDescriptor
import kotlinw.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

class RemotingSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) :
    SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols =
            resolver.getSymbolsWithAnnotation(SupportsRemoting::class.qualifiedName!!)
                .filterIsInstance<KSClassDeclaration>()
                .toSet()

        val validSymbols = symbols.filter { it.validate() }.toSet()
        val validSymbolsWithoutErrors = validSymbols.filter { validateAnnotatedClass(it) }
        validSymbolsWithoutErrors.forEach {
            generateProxyClass(it, resolver)
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

    private val ClassName.remoteCallDelegatorClassName: ClassName
        get() = ClassName(packageName, simpleName + "RemoteCallDelegator")

    private val KSTypeReference?.isUnit get() = this?.toTypeName() == Unit::class.asTypeName()

    private fun generateClientProxyClass(
        definitionInterfaceDeclaration: KSClassDeclaration,
        generatedFile: FileSpec.Builder,
        resolver: Resolver
    ) {
        val serializerFunctionName = MemberName("kotlinx.serialization", "serializer")

        val definitionInterfaceName = definitionInterfaceDeclaration.toClassName()
        val clientProxyClassName = definitionInterfaceName.clientProxyClassName

        val remotingClientParameterName = "remotingClient"
        val remotingClientSynchronousCallSupportPropertyName = "remotingClientSynchronousCallSupport"
        val remotingClientDownstreamFlowSupportPropertyName = "remotingClientDownstreamFlowSupport"
        val servicePathPropertyName = "servicePath"

        val processedMemberFunctions = definitionInterfaceDeclaration.getAllFunctions().toList()
            .filter { it.functionKind == FunctionKind.MEMBER && it.isAbstract }

        fun KSFunctionDeclaration.parameterClassName(nestedClassClassifier: Int) =
            clientProxyClassName.nestedClass("Parameter_" + simpleName.asString() + "_cid" + nestedClassClassifier)

        fun KSFunctionDeclaration.returnTypeName() = returnType?.toTypeName() ?: Nothing::class.asClassName()

        fun KSFunctionDeclaration.remotingMethodPath(nestedClassClassifier: Int) =
            simpleName.asString() + "_" + nestedClassClassifier // TODO customizable

        val servicePath = definitionInterfaceName.simpleName // TODO customizable

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
                        remotingClientSynchronousCallSupportPropertyName,
                        RemotingClientSynchronousCallSupport::class
                    )
                        .addModifiers(KModifier.PRIVATE)
                        .getter(
                            FunSpec.getterBuilder().addStatement(
                                "return $remotingClientParameterName as %T",
                                RemotingClientSynchronousCallSupport::class
                            ).build()
                        ) // TODO runtime hibaell.
                        .build()
                )
                .addProperty(
                    PropertySpec.builder(
                        remotingClientDownstreamFlowSupportPropertyName,
                        RemotingClientDownstreamFlowSupport::class
                    )
                        .addModifiers(KModifier.PRIVATE)
                        .getter(
                            FunSpec.getterBuilder().addStatement(
                                "return $remotingClientParameterName as %T",
                                RemotingClientDownstreamFlowSupport::class
                            ).build()
                        ) // TODO runtime hibaell.
                        .build()
                )
                .addProperty(
                    PropertySpec.builder(servicePathPropertyName, String::class)
                        .addModifiers(KModifier.PRIVATE)
                        .initializer("\"$servicePath\"")
                        .build()
                )

            processedMemberFunctions.forEachIndexed { nestedClassClassifier, function ->
                val functionName = function.simpleName.asString()
                val returnType = function.returnType
                val returnsFlow =
                    (returnType?.resolve()?.declaration as? KSClassDeclaration)?.toClassName() == Flow::class.asClassName()
                val parameters = function.parameters
                val hasParameters = parameters.isNotEmpty()
                val parameterClassName = function.parameterClassName(nestedClassClassifier)
                val resultClassName = function.returnTypeName()

                builder.addType(buildPayloadClass(parameterClassName, function.parameters))

                val funBuilder = FunSpec.builder(functionName)
                    .addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
                    .addParameters(
                        function.parameters.map {
                            ParameterSpec.builder(it.name!!.asString(), it.type.toTypeName()).build()
                        }
                    )

                if (returnType != null) {
                    val methodPath = function.remotingMethodPath(nestedClassClassifier)

                    if (returnsFlow) {
                        funBuilder.addStatement(
                            """
                                return $remotingClientDownstreamFlowSupportPropertyName.requestDownstreamColdFlow(
                                    %T::class, 
                                    %T::%N,
                                    $servicePathPropertyName,
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
                                ${if (returnType.isUnit) "" else "return "}$remotingClientSynchronousCallSupportPropertyName.call<%T, %T, %T>(
                                    %T::class, 
                                    %T::%N,
                                    $servicePathPropertyName,
                                    "$methodPath",
                                    %T${if (hasParameters) "(${parameters.joinToString { it.name!!.asString() }})" else ""},
                                    %M<%T>(),
                                    %M<%T>()
                                )
                            """.trimIndent(),
                            definitionInterfaceName, parameterClassName, resultClassName,
                            definitionInterfaceName,
                            definitionInterfaceName, functionName,
                            parameterClassName,
                            serializerFunctionName, parameterClassName,
                            serializerFunctionName, resultClassName
                        )
                    }
                } else {
                    funBuilder.addStatement("return TODO(\"Cannot be implemented until other errors are resolved.\")")
                }

                if (returnType != null) {
                    funBuilder.returns(returnType.toTypeName())
                }

                builder.addFunction(funBuilder.build())
            }

            return builder.build()
        }

        fun generateServerDelegateClass(): TypeSpec {
            val builder = TypeSpec.classBuilder(definitionInterfaceName.remoteCallDelegatorClassName)
                .addOriginatingKSFile(definitionInterfaceDeclaration.containingFile!!)
                .addModifiers(KModifier.PRIVATE)
                .addSuperinterface(RemoteCallDelegator::class)
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
                    PropertySpec.builder("servicePath", String::class)
                        .addModifiers(KModifier.OVERRIDE)
                        .initializer("\"$servicePath\"")
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
                                        function.returnTypeName()
                                    )
                                }
                            }
                            add(")\n.associateBy { it.memberId }")
                        }.build()
                    )
            builder.addProperty(methodDescriptorsBuilder.build())

            val processCallFunctionBuilder = FunSpec.builder("processCall")
                .addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
                .returns(Any::class)
                .addParameter("methodId", String::class)
                .addParameter("parameter", Any::class)

            processCallFunctionBuilder.beginControlFlow("return when (methodId)")

            processedMemberFunctions.forEachIndexed { nestedClassIdentifier, function ->
                val functionName = function.simpleName.asString()

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

                processCallFunctionBuilder.addStatement(buildTargetFunctionCall(), functionName)

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
            FunSpec.builder("remoteCallDelegator")
                .receiver(definitionInterfaceName.nestedClass("Companion"))
                .addParameter("target", definitionInterfaceName)
                .returns(RemoteCallDelegator::class)
                .addStatement("return %T(target)", definitionInterfaceName.remoteCallDelegatorClassName)
                .build()
        )
    }

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

    private fun validateAnnotatedClass(classDeclaration: KSClassDeclaration): Boolean {
        if (classDeclaration.classKind != ClassKind.INTERFACE) {
            logger.error(
                "Only interface declarations should be annotated with @${SupportsRemoting::class.simpleName}.",
                classDeclaration
            )
            return false
        }

        // TODO("Not yet implemented")
        return true
    }
}
