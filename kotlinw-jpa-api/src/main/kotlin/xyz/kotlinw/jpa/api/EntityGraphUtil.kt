package xyz.kotlinw.jpa.api

import jakarta.persistence.EntityGraph
import jakarta.persistence.Subgraph
import kotlin.reflect.KProperty1

// TODO továbbiak

fun <E : Any, S : Any> EntityGraph<E>.addSubgraph(
    property: KProperty1<E, S>,
    block: Subgraph<S>.() -> Unit = {}
): Subgraph<S> =
    addSubgraph<S>(property.name).apply(block)

fun <G : Any, S : Any> Subgraph<G>.addSubgraph(
    property: KProperty1<G, S>,
    block: Subgraph<S>.() -> Unit = {}
): Subgraph<S> =
    addSubgraph<S>(property.name).apply(block)
