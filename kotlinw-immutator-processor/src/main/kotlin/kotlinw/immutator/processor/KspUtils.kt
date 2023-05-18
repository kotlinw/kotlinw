package kotlinw.immutator.processor

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier

val KSClassDeclaration.companionObjectOrNull: KSClassDeclaration?
    get() = declarations.filterIsInstance<KSClassDeclaration>().firstOrNull { it.isCompanionObject }

val KSClassDeclaration.hasCompanionObject: Boolean
    get() = companionObjectOrNull != null

val KSClassDeclaration.isEnumClass get() = modifiers.contains(Modifier.ENUM)
