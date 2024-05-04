package xyz.kotlinw.jpa.api

import kotlin.reflect.KClass

@Deprecated(
    message = "Use the type-safe query creation functions.",
    replaceWith = ReplaceWith(
        "this.createTypeSafeQuery(qlString, resultClass)",
        imports = ["xyz.kotlinw.jpa.core.createTypeSafeQuery"]
    )
)
fun <T : Any> TypeSafeEntityManager.createQuery(qlString: String, resultClass: KClass<T>): TypeSafeQuery<T> =
    createQuery(qlString, resultClass.java)

@Deprecated(
    message = "Use the type-safe query creation functions.",
    replaceWith = ReplaceWith(
        "this.createTypeSafeNamedQuery(qlString, resultClass)",
        imports = ["xyz.kotlinw.jpa.core.createTypeSafeQuery"]
    )
)
fun <T : Any> TypeSafeEntityManager.createNamedQuery(name: String, resultClass: KClass<T>): TypeSafeQuery<T> =
    createNamedQuery(name, resultClass.java)
