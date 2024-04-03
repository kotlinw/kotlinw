package xyz.kotlinw.util.stdlib

import kotlinx.serialization.Serializable

@Serializable
data class NetworkAddress(val address: String, val port: Int)
