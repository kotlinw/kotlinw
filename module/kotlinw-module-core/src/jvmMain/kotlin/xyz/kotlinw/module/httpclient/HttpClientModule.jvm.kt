package xyz.kotlinw.module.httpclient

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.cio.CIO

internal actual fun getPlatformHttpClientEngine(): HttpClientEngineFactory<*> = CIO
