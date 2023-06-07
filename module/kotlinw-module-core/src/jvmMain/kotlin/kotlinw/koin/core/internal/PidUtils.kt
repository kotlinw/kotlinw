package kotlinw.koin.core.internal

import java.io.File

const val applicationPidFileName = "pid"

fun pidFile() = System.getenv("KOTLINW_APPLICATION_BASE_DIRECTORY")?.let { File(it, applicationPidFileName) }

fun File.readPid() = readText().toLong()

// TODO move to a better project/place
fun createPidFile() {
    pidFile()?.also {
        if (it.exists()) {
            it.delete()
        }

        it.writeText(ProcessHandle.current().pid().toString())
    }
}

// TODO move to a better project/place
fun deletePidFile() {
    pidFile()?.delete()
}
