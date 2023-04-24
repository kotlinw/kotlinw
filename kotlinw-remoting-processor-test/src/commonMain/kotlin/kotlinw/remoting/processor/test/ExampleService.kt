package kotlinw.remoting.processor.test

import kotlinw.remoting.api.SupportsRemoting

@SupportsRemoting
interface ExampleService {

    suspend fun noParameterReturnsUnit()

    suspend fun noParameterReturnsString(): String

    suspend fun p1IntReturnsUnit(p1: Int)

    suspend fun p1IntReturnsString(p1: Int): String

    suspend fun p1IntP2DoubleReturnsFloat(p1: Int, p2: Double): Float
}
