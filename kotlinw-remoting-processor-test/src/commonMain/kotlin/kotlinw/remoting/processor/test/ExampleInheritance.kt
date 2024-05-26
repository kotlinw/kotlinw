package kotlinw.remoting.processor.test

import kotlinx.coroutines.flow.Flow
import xyz.kotlinw.remoting.api.SupportsRemoting

@SupportsRemoting
interface GenericRemoteService {

    companion object;

    suspend fun greet(name: String): String
}

@SupportsRemoting
interface PersistentRemoteService: GenericRemoteService {

    companion object;

    suspend fun counterFlow(): Flow<Int>
}
