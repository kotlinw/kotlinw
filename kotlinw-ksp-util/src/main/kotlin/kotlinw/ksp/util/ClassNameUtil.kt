package kotlinw.ksp.util

import com.squareup.kotlinpoet.ClassName

fun ClassName.companionClassName() = nestedClass("Companion")
