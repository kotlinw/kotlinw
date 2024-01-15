package xyz.kotlinw.module.httpclient

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory

fun interface DefaultHttpClientEngineProvider {

    fun getDefaultHttpClientEngine(): HttpClientEngineFactory<*>
}
