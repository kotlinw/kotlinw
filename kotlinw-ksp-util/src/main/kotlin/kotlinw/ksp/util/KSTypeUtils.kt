package kotlinw.ksp.util

import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.typeNameOf

fun Resolver.anyReference() = createKSTypeReferenceFromKSType(builtIns.anyType)

inline fun <reified T : Annotation> KSType.getAnnotationsOfType() = declaration.getAnnotationsOfType<T>()

inline fun <reified T : Any> KSAnnotation.getArgumentOrNull(name: String) =
    arguments.firstOrNull { it.name!!.asString() == name }

inline fun <reified T : Any> KSAnnotation.getArgumentValueOrNull(name: String) =
    getArgumentOrNull<T>(name)?.value as? T
