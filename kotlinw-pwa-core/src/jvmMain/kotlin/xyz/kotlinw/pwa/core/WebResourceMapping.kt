package xyz.kotlinw.pwa.core

interface WebResourceMapping: WebResourceLookup {

    val authorizationProviderId: String?
}
