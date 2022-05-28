@file:OptIn(KspExperimental::class)

package kotlinw.immutator.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.ClassKind.INTERFACE
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Variance
import kotlinw.immutator.api.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.reflect.KClass

val KSType.isImmutable: Boolean
    get() =
        when (val typeDeclaration = declaration) {
            is KSClassDeclaration -> typeDeclaration.isImmutable || isSupportedImmutableCollection
            else -> false
        }

private val supportedImmutableCollectionClassNames: ImmutableSet<String> =
    listOf(ImmutableList::class, ImmutableMap::class)
        .map { it.qualifiedName!! }
        .toImmutableSet()

private val supportedCollectionVariances = setOf(Variance.COVARIANT, Variance.INVARIANT)

private val KSType.isSupportedImmutableCollection: Boolean
    get() = supportedImmutableCollectionClassNames.contains(declaration.qualifiedName?.asString())
            && arguments.all { it.type?.resolve()?.isImmutable ?: false && it.variance in supportedCollectionVariances }

val KSClassDeclaration.isImmutable: Boolean
    get() = knownImmutableClassNames.contains(qualifiedName?.asString()) ||
            isEnumClass ||
            isImmutableCustomClass ||
            isAnnotationPresent(Immutable::class)

private val knownImmutableClassNames: ImmutableSet<String> =
    listOf(
        String::class,
        Boolean::class,
        Char::class,
        Byte::class,
        Short::class,
        Int::class,
        Long::class,
        Float::class,
        Double::class,

        LocalDate::class,
        LocalDateTime::class,
        Instant::class
    )
        .map { it.qualifiedName!! }
        .toImmutableSet()

private val KSClassDeclaration.isEnumClass: Boolean
    get() = modifiers.contains(Modifier.ENUM)

private val KSClassDeclaration.isImmutableCustomClass: Boolean
    get() =
        getDeclaredProperties().all { !it.isMutable && !it.isDelegated() && it.type.resolve().isImmutable } &&
                (!modifiers.contains(Modifier.SEALED) || getSealedSubclasses().all { it.isImmutable }) &&
                superTypes.all {
                    val superTypeDeclaration = it.resolve().declaration
                    (superTypeDeclaration is KSClassDeclaration && superTypeDeclaration.classKind == INTERFACE) ||
                            superTypeDeclaration.modifiers.contains(Modifier.SEALED)
                }
