package xyz.kotlinw.jpa.api

import kotlin.reflect.KClass

@Deprecated(
    message = "Use the type-safe query creation functions.",
    replaceWith = ReplaceWith(
        "this.createTypedQuery(qlString, resultClass)",
        imports = ["xyz.kotlinw.jpa.core.createTypedQuery"]
    )
)
fun <T : Any> TypedEntityManager.createQuery(qlString: String, resultClass: KClass<T>): TypedQuery<T> =
    createQuery(qlString, resultClass.java)

@Deprecated(
    message = "Use the type-safe query creation functions.",
    replaceWith = ReplaceWith(
        "this.createTypedNamedQuery(qlString, resultClass)",
        imports = ["xyz.kotlinw.jpa.core.createTypedQuery"]
    )
)
fun <T : Any> TypedEntityManager.createNamedQuery(name: String, resultClass: KClass<T>): TypedQuery<T> =
    createNamedQuery(name, resultClass.java)
