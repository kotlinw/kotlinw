package kotlinw.immutator.processor

import com.google.devtools.ksp.symbol.KSClassDeclaration

val KSClassDeclaration.companionObjectOrNull: KSClassDeclaration?
    get() = declarations.filterIsInstance<KSClassDeclaration>().firstOrNull { it.isCompanionObject }

val KSClassDeclaration.hasCompanionObject: Boolean
    get() = companionObjectOrNull != null
