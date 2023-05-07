package kotlinw.remoting.processor.test

import kotlinw.remoting.api.SupportsRemoting
import kotlinx.coroutines.flow.Flow

@SupportsRemoting
interface ExampleService {

    companion object;

    suspend fun coldFlow(): Flow<Double>

    suspend fun numberFlow(offset: Int): Flow<Int>

    suspend fun nullableFlow(): Flow<String?>

    suspend fun noParameterReturnsUnit()

    suspend fun noParameterReturnsString(): String

    suspend fun p1IntReturnsUnit(p1: Int)

    suspend fun p1IntReturnsString(p1: Int): String

    suspend fun p1IntP2DoubleReturnsFloat(p1: Int, p2: Double): Float

    suspend fun noParameterReturnsNullableString(): String?

    suspend fun p1NullableIntParameterReturnsNullableString(p1: Int?): String?
}
