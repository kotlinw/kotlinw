package xyz.kotlinw.module.httpclient

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.java.Java

internal actual fun getPlatformHttpClientEngine(): HttpClientEngineFactory<*> = Java
