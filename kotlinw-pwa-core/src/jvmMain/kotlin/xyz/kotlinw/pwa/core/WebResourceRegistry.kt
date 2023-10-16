package xyz.kotlinw.pwa.core

import kotlinx.collections.immutable.ImmutableList

interface WebResourceRegistry {

    val webResourceMappings: ImmutableList<WebResourceMapping>
}
