package xyz.kotlinw.module.auth.core

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class PermissionKey(val value: String)
