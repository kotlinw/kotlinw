package test

import io.ktor.client.HttpClient
import kotlinw.koin.core.api.koinCoreModule
import kotlinw.module.koin.clikt.CliktApplicationCommand
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.dsl.module

class TestCommand: CliktApplicationCommand() {

    override fun Koin.runCommand() {
        val a by inject<HttpClient>()
        println(a)
    }
}
