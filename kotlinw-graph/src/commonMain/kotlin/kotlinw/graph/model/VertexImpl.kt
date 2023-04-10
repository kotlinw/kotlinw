package kotlinw.graph.model

import kotlin.jvm.JvmInline

@JvmInline
internal value class VertexImpl<V>(override val data: V): Vertex<V>
