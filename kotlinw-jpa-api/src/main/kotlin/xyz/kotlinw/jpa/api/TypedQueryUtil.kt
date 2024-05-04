package xyz.kotlinw.jpa.api

import jakarta.persistence.TypedQuery

fun <T, Q : TypedQuery<T>> Q.setParameters(arguments: Array<*>): Q {
    arguments.forEachIndexed { index, value ->
        setParameter(index + 1, value)
    }
    return this
}
