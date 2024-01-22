package kotlinw.util.stdlib

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.serialization.Serializable

@Serializable
data class SerializableResult<out V, out E>(
    val value: V?,
    val error: E?
) {

    fun toResult(): Result<V, E> =
        if (value != null) {
            Ok(value)
        } else if (error != null) {
            Err(error)
        } else {
            throw IllegalStateException()
        }
}
