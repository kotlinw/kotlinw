package kotlinw.remoting.processor.test

import arrow.core.raise.Raise
import arrow.core.raise.fold
import arrow.core.raise.recover
import xyz.kotlinw.remoting.api.SupportsRemoting

@SupportsRemoting
interface ExampleRaiseService {

    companion object;

    context(arrow.core.raise.Raise<kotlin.String>)
    @Suppress("RemoveRedundantQualifierName")
    suspend fun withRaise(): Int

    context(arrow.core.raise.Raise<kotlin.String>, arrow.core.raise.Raise<kotlin.Boolean>)
    @Suppress("RemoveRedundantQualifierName")
    suspend fun withMultipleRaises(): Int
}
