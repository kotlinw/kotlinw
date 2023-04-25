package kotlinw.remoting.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.FunctionKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import kotlinw.remoting.api.SupportsRemoting
import kotlinw.remoting.api.client.ClientProxy
import kotlinw.remoting.api.client.RemotingClient
import kotlinw.remoting.client.core.RemotingClientImplementor
import kotlinw.remoting.server.core.RawMessage
import kotlinw.remoting.server.core.RemoteCallDelegator
import kotlinw.remoting.server.core.MessageCodec
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

        validSymbols.forEach {
            validateAnnotatedClass(it)
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
        generateClientProxyClass(definitionInterfaceDeclaration, generatedFile)
        generatedFile.build().writeTo(codeGenerator, false)
    }

    private val ClassName.clientProxyClassName: ClassName
        get() = ClassName(packageName, simpleName + "ClientProxy")

    private val ClassName.remoteCallDelegatorClassName: ClassName
        get() = ClassName(packageName, simpleName + "RemoteCallDelegator")

    private val KSTypeReference?.isUnit get() = this?.toTypeName() == Unit::class.asTypeName()

    private fun generateClientProxyClass(
        definitionInterfaceDeclaration: KSClassDeclaration,
        generatedFile: FileSpec.Builder
    ) {
        val serializerFunctionName = MemberName("kotlinx.serialization", "serializer")

        val definitionInterfaceName = definitionInterfaceDeclaration.toClassName()
        val clientProxyClassName = definitionInterfaceName.clientProxyClassName

        val remotingClientParameterName = "remotingClient"
        val remotingClientImplementorPropertyName = "remotingClientImplementor"
        val servicePathPropertyName = "servicePath"

        val processedMemberFunctions = definitionInterfaceDeclaration.getAllFunctions().toList()
            .filter { it.functionKind == FunctionKind.MEMBER && it.isAbstract }

        fun KSFunctionDeclaration.parameterClassName(nestedClassClassifier: Int) =
            clientProxyClassName.nestedClass("Parameter_" + simpleName.asString() + "_cid" + nestedClassClassifier)

        fun KSFunctionDeclaration.resultClassName(nestedClassClassifier: Int) =
            clientProxyClassName.nestedClass("Result_" + simpleName.asString() + "_cid" + nestedClassClassifier)

        fun KSFunctionDeclaration.remotingMethodPath(nestedClassClassifier: Int) =
            simpleName.asString() + "_" + nestedClassClassifier // TODO customizable

        val servicePath = definitionInterfaceName.simpleName // TODO customizable

        fun generateClientProxyClass(): TypeSpec {
            val builder = TypeSpec.classBuilder(clientProxyClassName)
                .addOriginatingKSFile(definitionInterfaceDeclaration.containingFile!!)
                .addSuperinterface(definitionInterfaceName)
                .addSuperinterface(ClientProxy::class.asClassName().parameterizedBy(definitionInterfaceName))
                .primaryConstructor(
                    FunSpec.constructorBuilder()
                        .addParameter(remotingClientParameterName, RemotingClient::class)
                        .build()
                )
                .addProperty(
                    PropertySpec.builder(remotingClientImplementorPropertyName, RemotingClientImplementor::class)
                        .addModifiers(KModifier.PRIVATE)
                        .initializer("""$remotingClientParameterName as ${RemotingClientImplementor::class.qualifiedName}""")
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
                val parameters = function.parameters
                val hasParameters = parameters.isNotEmpty()
                val parameterClassName = function.parameterClassName(nestedClassClassifier)
                val resultClassName = function.resultClassName(nestedClassClassifier)

                builder.addType(buildPayloadClass(parameterClassName, function.parameters))

                if (returnType != null) {
                    builder.addType(
                        buildPayloadClass(
                            function.resultClassName(nestedClassClassifier),
                            if (returnType.isUnit) emptyMap() else mapOf("returnValue" to returnType)
                        )
                    )
                }

                val funBuilder = FunSpec.builder(functionName)
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameters(
                        function.parameters.map {
                            ParameterSpec.builder(it.name!!.asString(), it.type.toTypeName()).build()
                        }
                    )

                if (returnType != null) {
                    val methodPath = function.remotingMethodPath(nestedClassClassifier)
                    funBuilder.addStatement(
                        """
                        ${if (returnType.isUnit) "" else "return "}$remotingClientImplementorPropertyName.call<%T, %T, %T>(
                            %T::class, 
                            %T::%N,
                            $servicePathPropertyName,
                            "$methodPath",
                            %T${if (hasParameters) "(${parameters.joinToString { it.name!!.asString() }})" else ""},
                            %M<%T>(),
                            %M<%T>()
                        )${if (returnType.isUnit) "" else ".returnValue"}
                    """.trimIndent(),
                        definitionInterfaceName, parameterClassName, resultClassName, // call<...> type arguments
                        definitionInterfaceName, // T::class
                        definitionInterfaceName, functionName, // T::function
                        parameterClassName, // parameter value
                        serializerFunctionName, parameterClassName, // serializer
                        serializerFunctionName, resultClassName // deserializer
                    )

                    if (returnType.isUnit) {
                        funBuilder.addStatement("return Unit")
                    }
                } else {
                    funBuilder.addStatement("return TODO(\"Cannot be implemented until other errors are resolved.\")")
                }

                if (function.modifiers.contains(Modifier.SUSPEND)) {
                    funBuilder.addModifiers(KModifier.SUSPEND)
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

            val processCallFunctionBuilder = FunSpec.builder("processCall")
                .addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
                .returns(RawMessage::class)
                .addParameter("methodPath", String::class)
                .addParameter("requestData", RawMessage::class)
                .addParameter("messageCodec", MessageCodec::class)

            processCallFunctionBuilder.beginControlFlow("return when(methodPath)")

            processedMemberFunctions.forEachIndexed { nestedClassIdentifier, function ->
                val returnType = function.returnType
                val functionName = function.simpleName.asString()

                processCallFunctionBuilder.beginControlFlow("%S ->", function.remotingMethodPath(nestedClassIdentifier))
                processCallFunctionBuilder.addStatement(
                    "val r = messageCodec.decodeMessage(requestData, %M<%T>())",
                    serializerFunctionName,
                    function.parameterClassName(nestedClassIdentifier)
                )

                fun buildTargetFunctionCall() = "target.%N(${function.parameters.joinToString { "r." + it.name!!.asString() }})"

                if (returnType.isUnit) {
                    processCallFunctionBuilder.addStatement(buildTargetFunctionCall(), functionName)
                    processCallFunctionBuilder.addStatement(
                        "messageCodec.encodeMessage(%T, %M<%T>())",
                        function.resultClassName(nestedClassIdentifier),
                        serializerFunctionName,
                        function.resultClassName(nestedClassIdentifier),
                    )
                } else {
                    processCallFunctionBuilder.addStatement(
                        "messageCodec.encodeMessage(%T(${buildTargetFunctionCall()}), %M<%T>())",
                        function.resultClassName(nestedClassIdentifier),
                        functionName,
                        serializerFunctionName,
                        function.resultClassName(nestedClassIdentifier),
                    )
                }

                processCallFunctionBuilder.endControlFlow()
            }

            processCallFunctionBuilder.beginControlFlow("else ->")
            processCallFunctionBuilder.addStatement("throw IllegalStateException(\"Invalid method path: ${"$"}methodPath\")")
            processCallFunctionBuilder.endControlFlow()
            processCallFunctionBuilder.endControlFlow()

            builder.addFunction(processCallFunctionBuilder.build())

            return builder.build()
        }

        generatedFile.addType(generateClientProxyClass())
        generatedFile.addType(generateServerDelegateClass())
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

    private fun validateAnnotatedClass(declaration: KSClassDeclaration) {
        // TODO("Not yet implemented")
    }
}