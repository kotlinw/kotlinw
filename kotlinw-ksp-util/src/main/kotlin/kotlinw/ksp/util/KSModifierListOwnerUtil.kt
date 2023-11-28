package kotlinw.ksp.util

import com.google.devtools.ksp.symbol.KSModifierListOwner
import com.google.devtools.ksp.symbol.Modifier

val KSModifierListOwner.isSuspend get() = Modifier.SUSPEND in modifiers
