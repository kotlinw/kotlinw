package xyz.kotlinw.jpa.api

import jakarta.persistence.NoResultException
import jakarta.persistence.TypedQuery

fun <T, Q : TypedQuery<T>> Q.setParameters(arguments: Array<*>): Q {
    arguments.forEachIndexed { index, value ->
        setParameter(index + 1, value)
    }
    return this
}

fun <R : Any> TypedQuery<R>.getSingleResultOrNull(): R? =
    try {
        singleResult
    } catch (e: NoResultException) {
        null
    }
