package kotlinw.hibernate.core.api

import jakarta.persistence.EntityGraph
import jakarta.persistence.Subgraph
import kotlin.reflect.KProperty1

inline fun <reified E : Any> TypeSafeEntityManager.createEntityGraph() = createEntityGraph(E::class.java)

fun <E : Any, S : Any> EntityGraph<E>.addSubgraph(property: KProperty1<E, S>): Subgraph<S> = addSubgraph(property.name)

fun <G : Any, S : Any> Subgraph<G>.addSubgraph(property: KProperty1<G, S>): Subgraph<G> = addSubgraph(property.name)
