package kotlinw.ksp.util

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.typeNameOf

val KSClassDeclaration.companionObjectOrNull: KSClassDeclaration?
    get() = declarations.filterIsInstance<KSClassDeclaration>().firstOrNull { it.isCompanionObject }

val KSClassDeclaration.hasCompanionObject: Boolean
    get() = companionObjectOrNull != null

val KSClassDeclaration.isEnumClass get() = modifiers.contains(Modifier.ENUM)

inline fun <reified T : Annotation> KSDeclaration.getAnnotationsOfType() =
    annotations.filter { it.annotationType.toTypeName() == typeNameOf<T>() }
