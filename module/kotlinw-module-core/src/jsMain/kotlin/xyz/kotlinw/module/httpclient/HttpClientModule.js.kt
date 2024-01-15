package xyz.kotlinw.module.httpclient

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.js.Js

internal actual fun getPlatformHttpClientEngine(): HttpClientEngineFactory<*> = Js
