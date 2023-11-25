package kotlinw.ksp.util

import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.typeNameOf

fun Resolver.anyReference() = createKSTypeReferenceFromKSType(builtIns.anyType)

inline fun <reified T : Annotation> KSType.getAnnotationsOfType() = declaration.getAnnotationsOfType<T>()

fun KSAnnotation.getArgumentOrNull(name: String) = arguments.firstOrNull { it.name!!.asString() == name }
