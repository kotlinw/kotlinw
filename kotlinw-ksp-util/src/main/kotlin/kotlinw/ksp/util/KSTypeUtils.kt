package kotlinw.ksp.util

import com.google.devtools.ksp.processing.KSBuiltIns
import com.google.devtools.ksp.processing.Resolver

fun Resolver.anyReference() = createKSTypeReferenceFromKSType(builtIns.anyType)
