package kotlinw.remoting.client.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.FunctionKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
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
import kotlinx.serialization.Serializable

class RemotingClientSymbolProcessor(
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
            definitionInterfaceDeclaration.clientProxyClassSimpleName
        )
        generateClientProxyClass(definitionInterfaceDeclaration, generatedFile, resolver)
        generatedFile.build().writeTo(codeGenerator, false)
    }

    private val KSClassDeclaration.clientProxyClassSimpleName: String get() = toClassName().simpleName + "ClientProxy"

    private val KSClassDeclaration.clientProxyClassQualifiedName: String
        get() = buildString {
            val packageName = toClassName().packageName
            if (packageName.isNotEmpty()) {
                append(packageName)
                append(".")
            }
            append(clientProxyClassSimpleName)
        }

    private fun generateClientProxyClass(
        definitionInterfaceDeclaration: KSClassDeclaration,
        generatedFile: FileSpec.Builder,
        resolver: Resolver
    ) {
        val definitionInterfaceName = definitionInterfaceDeclaration.toClassName()
        val generatedClassName = ClassName.bestGuess(definitionInterfaceDeclaration.clientProxyClassQualifiedName)

        val remotingClientParameterName = "remotingClient"
        val remotingClientImplementorPropertyName = "remotingClientImplementor"
        val servicePathPropertyName = "servicePath"

        val processedMemberFunctions = definitionInterfaceDeclaration.getAllFunctions().toList()
            .filter { it.functionKind == FunctionKind.MEMBER && it.isAbstract }
        var nextNestedClassClassifier = 0
        val builder = TypeSpec.classBuilder(generatedClassName)
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
                    .initializer("""$remotingClientParameterName as ${RemotingClientImplementor::class.qualifiedName}""") // TODO customizable
                    .build()
            )
            .addProperty(
                PropertySpec.builder(servicePathPropertyName, String::class)
                    .addModifiers(KModifier.PRIVATE)
                    .initializer("%T::class.simpleName!!", definitionInterfaceName)
                    .build()
            )

        val serializerFunctionName = MemberName("kotlinx.serialization", "serializer")

        processedMemberFunctions.forEach { function ->
            val functionName = function.simpleName.asString()
            val returnType = function.returnType
            val parameters = function.parameters
            val hasParameters = parameters.isNotEmpty()

            val nestedClassClassifier = nextNestedClassClassifier++
            val parameterClassName =
                generatedClassName.nestedClass("Parameter_" + functionName + "_" + nestedClassClassifier)
            builder.addType(buildPayloadClass(parameterClassName, function.parameters))

            val resultClassName = generatedClassName.nestedClass("Result_" + functionName + "_" + nestedClassClassifier)
            if (returnType != null) {
                val isUnitReturnType = returnType.toTypeName() == Unit::class.asTypeName()
                builder.addType(
                    buildPayloadClass(
                        resultClassName,
                        if (isUnitReturnType) emptyMap() else mapOf("returnValue" to returnType)
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
                val isUnitReturnType = returnType.toTypeName() == Unit::class.asTypeName()
                val methodPath = functionName // TODO customizable
                funBuilder.addStatement(
                    """
                        ${if (isUnitReturnType) "" else "return "}$remotingClientImplementorPropertyName.call<%T, %T, %T>(
                            %T::class, 
                            %T::%N,
                            $servicePathPropertyName,
                            "$methodPath",
                            %T${if (hasParameters) "(${parameters.joinToString { it.name!!.asString() }})" else ""},
                            %M<%T>(),
                            %M<%T>()
                        )${if (isUnitReturnType) "" else ".returnValue"}
                    """.trimIndent(),
                    definitionInterfaceName, parameterClassName, resultClassName, // call<...> type arguments
                    definitionInterfaceName, // T::class
                    definitionInterfaceName, functionName, // T::function
                    parameterClassName, // parameter value
                    serializerFunctionName, parameterClassName, // serializer
                    serializerFunctionName, resultClassName // deserializer
                )

                if (isUnitReturnType) {
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


        generatedFile.addType(builder.build())
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
