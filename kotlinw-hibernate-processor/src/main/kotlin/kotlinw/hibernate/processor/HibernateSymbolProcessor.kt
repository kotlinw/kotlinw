package kotlinw.hibernate.processor;

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.AnnotationSpec.Companion
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import com.squareup.kotlinpoet.typeNameOf
import jakarta.persistence.Entity
import kotlin.reflect.KClass
import kotlinw.hibernate.api.configuration.PersistentClassProvider
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton

class HibernateSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {

    companion object {

        private val entityAnnotationQualifiedName = Entity::class.qualifiedName!!

        private val entityAnnotationDisplayName = "@${Entity::class.simpleName}"

        internal const val generatedClassSimpleName = "GeneratedPackagePersistentClassProvider"
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val annotatedSymbols = resolver
            .getSymbolsWithAnnotation(entityAnnotationQualifiedName)

        val processedSymbols =
            annotatedSymbols
                .filter { it.validate() }
                .toList()

        processedSymbols
            .filter { annotatedSymbol ->
                if (annotatedSymbol !is KSClassDeclaration) {
                    logger.error(
                        "Declaration annotated with $entityAnnotationDisplayName should be a class declaration.",
                        annotatedSymbol
                    )
                    false
                } else if (annotatedSymbol.classKind != ClassKind.CLASS) {
                    logger.error(
                        "Class declaration annotated with $entityAnnotationDisplayName should be a class.",
                        annotatedSymbol
                    )
                    false
                } else {
                    true
                }
            }
            .filterIsInstance<KSClassDeclaration>()
            .groupBy { it.packageName.asString() }
            .forEach { (packageName, entityClassDeclarations) ->
                FileSpec.builder(packageName, generatedClassSimpleName)
                    .addType(
                        TypeSpec.classBuilder(generatedClassSimpleName)
                            .addAnnotation(Singleton::class.asClassName())
                            .addAnnotation(
                                AnnotationSpec.builder(Named::class.asClassName())
                                    .addMember("%L = %S", "value", "$packageName.$generatedClassSimpleName")
                                    .build()
                            )
                            .addSuperinterface(PersistentClassProvider::class)
                            .addFunction(
                                FunSpec.builder("getPersistentClasses")
                                    .addModifiers(KModifier.OVERRIDE)
                                    .returns(typeNameOf<List<KClass<*>>>())
                                    .addStatement(
                                        """
                                            return %M(${entityClassDeclarations.joinToString { "%T::class" }})
                                        """.trimIndent(),
                                        MemberName("kotlin.collections", "listOf"),
                                        *entityClassDeclarations.map { it.toClassName() }.toTypedArray()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
                    .writeTo(codeGenerator, true, entityClassDeclarations.map { it.containingFile }.filterNotNull())
            }

        return annotatedSymbols.filter { !it.validate() }.toList()
    }
}
