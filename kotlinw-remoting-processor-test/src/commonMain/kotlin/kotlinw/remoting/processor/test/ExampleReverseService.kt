package kotlinw.remoting.processor.test

import xyz.kotlinw.remoting.api.SupportsRemoting

@SupportsRemoting
interface ExampleReverseService {

    companion object;

    suspend fun add(a: Int, b: Int): Int
}
