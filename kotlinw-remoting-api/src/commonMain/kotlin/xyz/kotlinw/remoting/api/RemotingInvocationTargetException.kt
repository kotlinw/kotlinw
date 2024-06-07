package xyz.kotlinw.remoting.api

import kotlinx.coroutines.CancellationException

sealed interface RemotingInvocationTargetFailure

class RemotingInvocationTargetException(message: String?) : RuntimeException(message), RemotingInvocationTargetFailure

class RemotingInvocationTargetCancellationException(message: String?) : CancellationException(message),
    RemotingInvocationTargetFailure
