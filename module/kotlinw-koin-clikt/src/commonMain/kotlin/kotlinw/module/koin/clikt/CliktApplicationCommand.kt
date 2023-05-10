package kotlinw.module.koin.clikt

import com.github.ajalt.clikt.core.CliktCommand
import kotlinw.koin.core.api.coreKoinModule
import kotlinx.coroutines.runBlocking
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

abstract class CliktApplicationCommand(
    private val koinApplication: KoinApplication,
    help: String = "",
    epilog: String = "",
    name: String? = null,
    invokeWithoutSubcommand: Boolean = false,
    printHelpOnEmptyArgs: Boolean = false,
    helpTags: Map<String, String> = emptyMap(),
    autoCompleteEnvvar: String? = "",
    allowMultipleSubcommands: Boolean = false,
    treatUnknownOptionsAsArgs: Boolean = false,
    hidden: Boolean = false
) :
    CliktCommand(
        help,
        epilog,
        name,
        invokeWithoutSubcommand,
        printHelpOnEmptyArgs,
        helpTags,
        autoCompleteEnvvar,
        allowMultipleSubcommands,
        treatUnknownOptionsAsArgs,
        hidden
    ) {

    final override fun run() {
        runBlocking {
            koinApplication.koin.runCommand()
        }
    }

    protected abstract suspend fun Koin.runCommand()
}
