package kotlinw.graph.model

import kotlin.jvm.JvmInline

@JvmInline
internal value class VertexImpl<out D : Any>(override val data: D): Vertex<D>
