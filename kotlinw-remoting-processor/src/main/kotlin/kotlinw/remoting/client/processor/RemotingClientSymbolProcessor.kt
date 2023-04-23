package kotlinw.remoting.client.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import kotlinw.remoting.api.ClientProxy
import kotlinw.remoting.api.RemotingCapable
import kotlinw.remoting.api.RemotingClient
import kotlin.reflect.KProperty1

class RemotingClientSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) :
    SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols =
            resolver.getSymbolsWithAnnotation(RemotingCapable::class.qualifiedName!!)
                .filterIsInstance<KSClassDeclaration>()
                .toSet()

        val validSymbols = symbols.filter { it.validate() }.toSet()

        validSymbols.forEach {
            validateAnnotatedClass(it)
            generateProxyClass(it)
        }

        return (symbols - validSymbols).toList()
    }

    private fun generateProxyClass(definitionInterfaceDeclaration: KSClassDeclaration) {
        val definitionInterfaceName = definitionInterfaceDeclaration.toClassName()

        val generatedFile = FileSpec.builder(
            definitionInterfaceName.packageName,
            definitionInterfaceDeclaration.clientProxyClassSimpleName
        )
        generateClientProxyClass(definitionInterfaceDeclaration, generatedFile)
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
        generatedFile: FileSpec.Builder
    ) {
        val definitionInterfaceName = definitionInterfaceDeclaration.toClassName()
        val generatedClassName = ClassName.bestGuess(definitionInterfaceDeclaration.clientProxyClassQualifiedName)

        val builder = TypeSpec.classBuilder(generatedClassName)
            .addOriginatingKSFile(definitionInterfaceDeclaration.containingFile!!)
            .addSuperinterface(definitionInterfaceDeclaration.toClassName())
            .addSuperinterface(ClientProxy::class.asClassName().parameterizedBy(definitionInterfaceName))
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("remotingClient", RemotingClient::class)
                    .build()
            )
            .addProperty(
                PropertySpec.builder("remotingClient", RemotingClient::class)
                    .addModifiers(KModifier.PRIVATE)
                    .initializer("remotingClient")
                    .build()
            )
            .addFunctions(
                definitionInterfaceDeclaration.getAllFunctions().toList().map { overriddenFunction ->
                    val funBuilder = FunSpec.builder(overriddenFunction.simpleName.asString())
                        .addModifiers(KModifier.OVERRIDE)
                        .addCode("TODO()")

                    if (overriddenFunction.modifiers.contains(Modifier.SUSPEND)) {
                        funBuilder.addModifiers(KModifier.SUSPEND)
                    }

                    val returnType = overriddenFunction.returnType
                    if (returnType != null) {
                        funBuilder.returns(returnType.resolve().toTypeName())
                    }

                    overriddenFunction.parameters.forEach {
                        funBuilder.addParameter(it.name!!.asString(), it.type.resolve().toTypeName())
                    }

                    funBuilder.build()
                }
            )

        generatedFile.addType(builder.build())
    }


    private fun validateAnnotatedClass(declaration: KSClassDeclaration) {
        // TODO("Not yet implemented")
    }
}

//                .addProperty(
//                    PropertySpec.builder("source", immutableDataClassName)
//                        .addModifiers(KModifier.PRIVATE)
//                        .initializer("source")
//                        .build()
//                )
//                .primaryConstructor(
//                    FunSpec.constructorBuilder()
//                        .addParameter("source", immutableDataClassName)
//                        .build()
//                )
//                .addFunction(
//                    FunSpec.builder(convertToImmutableMethodName)
//                        .returns(immutableDataClassName)
//                        .addModifiers(KModifier.OVERRIDE)
//                        .addCode(
//                            """
//                            return if ($isModifiedPropertyName) {
//                             ${immutableDataClassName}(${
//                                definitionInterfaceDeclaration.abstractProperties.joinToString {
//                                    val propertyName = it.simpleName.asString()
//                                    when (it.type.resolve().propertyType) {
//                                        PropertyType.Value -> propertyName
//                                        PropertyType.Immutated -> if (it.type.resolve().isMarkedNullable) "$propertyName?.${convertToImmutableMethodName}()" else "$propertyName.${convertToImmutableMethodName}()"
//                                        PropertyType.Set -> TODO()
//                                        PropertyType.List -> if (it.type.resolve().isMarkedNullable) "$propertyName?.${convertToImmutableMethodName}()" else "$propertyName.${convertToImmutableMethodName}()"
//                                        PropertyType.Map -> if (it.type.resolve().isMarkedNullable) "$propertyName?.${convertToImmutableMethodName}()" else "$propertyName.${convertToImmutableMethodName}()"
//                                        PropertyType.Unsupported -> throwInternalCompilerError(
//                                            it.type.resolve()
//                                                .toString() + ", " + it.type.resolve().declaration.qualifiedName?.asString(),
//                                            it
//                                        ) // TODO
//                                    }
//                                }
//                            })
//                            } else {
//                                source
//                            }
//                            """.trimIndent()
//                        )
//                        .build()
//                )


//
//// Generated
//class ServiceClientProxy(private val client: RemotingClient) : Service, ClientProxy<Service> {
//
//    override suspend fun a() {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun convert(value: Int): String {
//        TODO("Not yet implemented")
//    }
//}
