package kotlinw.ksp.util

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.typeNameOf

val KSClassDeclaration.companionObjectOrNull: KSClassDeclaration?
    get() = declarations.filterIsInstance<KSClassDeclaration>().firstOrNull { it.isCompanionObject }

val KSClassDeclaration.hasCompanionObject: Boolean
    get() = companionObjectOrNull != null

val KSClassDeclaration.isEnumClass get() = modifiers.contains(Modifier.ENUM)

inline fun <reified T : Annotation> KSAnnotated.getAnnotationsOfType() = filterAnnotations<T>()

inline fun <reified T : Annotation> KSAnnotated.filterAnnotations() = annotations.filterAnnotations<T>()

inline fun <reified T : Annotation> Sequence<KSAnnotation>.filterAnnotations() =
    filter { it.annotationType.toTypeName() == typeNameOf<T>() }
