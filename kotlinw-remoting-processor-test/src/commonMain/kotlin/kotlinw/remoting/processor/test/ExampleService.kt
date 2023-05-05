package kotlinw.remoting.processor.test

import kotlinw.remoting.api.SupportsRemoting
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

@SupportsRemoting
interface ExampleService {

    companion object;

    // TODO val sharedFlow: SharedFlow<String>

    suspend fun coldFlow(): Flow<Double>

    suspend fun numberFlow(offset: Int): Flow<Int>

    suspend fun noParameterReturnsUnit()

    suspend fun noParameterReturnsString(): String

    suspend fun p1IntReturnsUnit(p1: Int)

    suspend fun p1IntReturnsString(p1: Int): String

    suspend fun p1IntP2DoubleReturnsFloat(p1: Int, p2: Double): Float
}
