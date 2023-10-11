package xyz.kotlinw.module.pwa.server

import xyz.kotlinw.io.RelativePath

internal const val reservedPathSegment = "x"

internal const val publicResourcesPathSegment = "p"

fun getModuleWebResourceBasePath(moduleId: String) = RelativePath("m/$moduleId")
