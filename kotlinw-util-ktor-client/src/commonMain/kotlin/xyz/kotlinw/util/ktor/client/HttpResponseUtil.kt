package xyz.kotlinw.util.ktor.client

import arrow.core.raise.Raise
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess

inline fun <T, E> HttpResponse.process(
    onSuccess: (HttpResponse) -> T,
    onError: (HttpResponse) -> E
) =
    if (status.isSuccess())
        Ok(onSuccess(this))
    else
        Err(onError(this))

context(Raise<E>)
inline fun <T, E> HttpResponse.process(
    onSuccess: context(Raise<E>) (HttpResponse) -> T,
    onError: context(Raise<E>) (HttpResponse) -> T
) =
    if (status.isSuccess())
        onSuccess(this@Raise, this)
    else
        onError(this@Raise, this)
