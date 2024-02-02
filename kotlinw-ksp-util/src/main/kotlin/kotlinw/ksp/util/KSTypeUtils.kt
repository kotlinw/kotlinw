package kotlinw.ksp.util

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.typeNameOf

fun Resolver.anyReference() = createKSTypeReferenceFromKSType(builtIns.anyType)

inline fun <reified T : Any> KSAnnotation.getArgumentOrNull(name: String) =
    arguments.firstOrNull { it.name!!.asString() == name }

inline fun <reified T : Any> KSAnnotation.getArgumentValueOrNull(name: String) =
    getArgumentOrNull<T>(name)?.value as? T
