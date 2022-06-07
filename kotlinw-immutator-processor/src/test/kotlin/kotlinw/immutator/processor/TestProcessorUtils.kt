package kotlinw.immutator.processor

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Variance
import com.tschuchort.compiletesting.SourceFile
import kotlinw.util.Uuid
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestProcessorUtils {
    @Test
    fun testIsImmutable() {
        compile(
            SourceFile.kotlin(
                "Test.kt",
                """
                import kotlinw.immutator.annotation.Immutate
                import kotlinw.immutator.test.simple.PetKind.Dog
                import kotlinw.immutator.test.simple.PetKind.Rabbit
                import kotlinw.immutator.util.mutate
                import kotlinx.collections.immutable.persistentListOf
                import kotlinx.datetime.LocalDate
                import java.time.Month.FEBRUARY
                import kotlin.test.Test
                import kotlin.test.assertNotSame
                import kotlin.test.assertSame
                
                // Inferred immutable class
                data class Address(
                    val city: String,
                    val street: String
                )
                
                @Immutate
                sealed interface Person {
                    val name: PersonName
                
                    val birthDate: LocalDate
                
                    val address: Address
                
                    val pets: List<Pet>
                }
                
                @Immutate
                sealed interface PersonName {
                    val title: String?
                
                    val firstName: String
                
                    val lastName: String
                
                    val fullName get() = (if (title != null) title + " " else "") + firstName + " " + lastName
                }
                
                enum class PetKind {
                    Dog, Cat, Rabbit
                }
                
                @Immutate
                sealed interface Pet {
                    val kind: PetKind
                
                    val name: String
                }
                """.trimIndent()
            ),
            object : SymbolProcessorProvider {
                override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
                    object : SymbolProcessor {
                        override fun process(resolver: Resolver): List<KSAnnotated> {
                            val stringTypeRef = resolver.createTypeReference<String>()

                            val listClassDeclaration = resolver.getClassDeclarationByName<List<*>>()!!
                            val listOfStringType = listClassDeclaration.asType(
                                listOf(resolver.getTypeArgument(stringTypeRef, Variance.INVARIANT))
                            )
                            assertFalse(listOfStringType.isImmutable)

                            val uuidTypeRef = resolver.createTypeReference<Uuid>()
                            assertTrue(uuidTypeRef.resolve().isImmutable)

                            val personClassDeclaration = resolver.getSymbolsWithAnnotation(annotationQualifiedName)
                                .first { it is KSClassDeclaration && it.simpleName.asString() == "Person" } as KSClassDeclaration
                            val petsProperty =
                                personClassDeclaration.getAllProperties().first { it.simpleName.asString() == "pets" }

                            return emptyList()
                        }
                    }

                inline fun <reified T> Resolver.createTypeReference(): KSTypeReference {
                    val classDeclaration = getClassDeclarationByName<T>()!!
                    val type = classDeclaration.asType(emptyList())
                    return createKSTypeReferenceFromKSType(type)
                }
            }
        )
    }
}
