package xyz.kotlinw.jpa.core

import xyz.kotlinw.jpa.api.TypedEntityManager
import xyz.kotlinw.jpa.api.TypedProjectionQuery10
import xyz.kotlinw.jpa.api.TypedProjectionQuery11
import xyz.kotlinw.jpa.api.TypedProjectionQuery12
import xyz.kotlinw.jpa.api.TypedProjectionQuery13
import xyz.kotlinw.jpa.api.TypedProjectionQuery14
import xyz.kotlinw.jpa.api.TypedProjectionQuery2
import xyz.kotlinw.jpa.api.TypedProjectionQuery3
import xyz.kotlinw.jpa.api.TypedProjectionQuery4
import xyz.kotlinw.jpa.api.TypedProjectionQuery5
import xyz.kotlinw.jpa.api.TypedProjectionQuery6
import xyz.kotlinw.jpa.api.TypedProjectionQuery7
import xyz.kotlinw.jpa.api.TypedProjectionQuery8
import xyz.kotlinw.jpa.api.TypedProjectionQuery9

//
// Projection query (query string)
//

fun <R1, R2> TypedEntityManager.createProjectionQuery2(qlString: String) =
    TypedProjectionQuery2<R1, R2>(createTypedQuery<Array<*>>(qlString))

fun <R1, R2, R3> TypedEntityManager.createProjectionQuery3(qlString: String) =
    TypedProjectionQuery3<R1, R2, R3>(createTypedQuery<Array<*>>(qlString))

fun <R1, R2, R3, R4> TypedEntityManager.createProjectionQuery4(qlString: String) =
    TypedProjectionQuery4<R1, R2, R3, R4>(createTypedQuery<Array<*>>(qlString))

fun <R1, R2, R3, R4, R5> TypedEntityManager.createProjectionQuery5(qlString: String) =
    TypedProjectionQuery5<R1, R2, R3, R4, R5>(createTypedQuery<Array<*>>(qlString))

fun <R1, R2, R3, R4, R5, R6> TypedEntityManager.createProjectionQuery6(qlString: String) =
    TypedProjectionQuery6<R1, R2, R3, R4, R5, R6>(createTypedQuery<Array<*>>(qlString))

fun <R1, R2, R3, R4, R5, R6, R7> TypedEntityManager.createProjectionQuery7(qlString: String) =
    TypedProjectionQuery7<R1, R2, R3, R4, R5, R6, R7>(createTypedQuery<Array<*>>(qlString))

fun <R1, R2, R3, R4, R5, R6, R7, R8> TypedEntityManager.createProjectionQuery8(qlString: String) =
    TypedProjectionQuery8<R1, R2, R3, R4, R5, R6, R7, R8>(createTypedQuery<Array<*>>(qlString))

fun <R1, R2, R3, R4, R5, R6, R7, R8, R9> TypedEntityManager.createProjectionQuery9(qlString: String) =
    TypedProjectionQuery9<R1, R2, R3, R4, R5, R6, R7, R8, R9>(createTypedQuery<Array<*>>(qlString))

fun <R1, R2, R3, R4, R5, R6, R7, R8, R9, R10> TypedEntityManager.createProjectionQuery10(qlString: String) =
    TypedProjectionQuery10<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10>(createTypedQuery<Array<*>>(qlString))

fun <R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11> TypedEntityManager.createProjectionQuery11(qlString: String) =
    TypedProjectionQuery11<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11>(createTypedQuery<Array<*>>(qlString))

fun <R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12> TypedEntityManager.createProjectionQuery12(qlString: String) =
    TypedProjectionQuery12<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12>(createTypedQuery<Array<*>>(qlString))

fun <R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13> TypedEntityManager.createProjectionQuery13(qlString: String) =
    TypedProjectionQuery13<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13>(
        createTypedQuery<Array<*>>(qlString)
    )

fun <R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13, R14> TypedEntityManager.createProjectionQuery14(qlString: String) =
    TypedProjectionQuery14<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13, R14>(
        createTypedQuery<Array<*>>(qlString)
    )

//
// Projection query (named query)
//

fun <R1, R2> TypedEntityManager.createProjectionNamedQuery2(name: String) =
    TypedProjectionQuery2<R1, R2>(createTypedNamedQuery<Array<*>>(name))

fun <R1, R2, R3> TypedEntityManager.createProjectionNamedQuery3(name: String) =
    TypedProjectionQuery3<R1, R2, R3>(createTypedNamedQuery<Array<*>>(name))

fun <R1, R2, R3, R4> TypedEntityManager.createProjectionNamedQuery4(name: String) =
    TypedProjectionQuery4<R1, R2, R3, R4>(createTypedNamedQuery<Array<*>>(name))

fun <R1, R2, R3, R4, R5> TypedEntityManager.createProjectionNamedQuery5(name: String) =
    TypedProjectionQuery5<R1, R2, R3, R4, R5>(createTypedNamedQuery<Array<*>>(name))

fun <R1, R2, R3, R4, R5, R6> TypedEntityManager.createProjectionNamedQuery6(name: String) =
    TypedProjectionQuery6<R1, R2, R3, R4, R5, R6>(createTypedNamedQuery<Array<*>>(name))

fun <R1, R2, R3, R4, R5, R6, R7> TypedEntityManager.createProjectionNamedQuery7(name: String) =
    TypedProjectionQuery7<R1, R2, R3, R4, R5, R6, R7>(createTypedNamedQuery<Array<*>>(name))

fun <R1, R2, R3, R4, R5, R6, R7, R8> TypedEntityManager.createProjectionNamedQuery8(name: String) =
    TypedProjectionQuery8<R1, R2, R3, R4, R5, R6, R7, R8>(createTypedNamedQuery<Array<*>>(name))

fun <R1, R2, R3, R4, R5, R6, R7, R8, R9> TypedEntityManager.createProjectionNamedQuery9(name: String) =
    TypedProjectionQuery9<R1, R2, R3, R4, R5, R6, R7, R8, R9>(createTypedNamedQuery<Array<*>>(name))

fun <R1, R2, R3, R4, R5, R6, R7, R8, R9, R10> TypedEntityManager.createProjectionNamedQuery10(name: String) =
    TypedProjectionQuery10<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10>(createTypedNamedQuery<Array<*>>(name))

fun <R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11> TypedEntityManager.createProjectionNamedQuery11(name: String) =
    TypedProjectionQuery11<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11>(createTypedNamedQuery<Array<*>>(name))

fun <R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12> TypedEntityManager.createProjectionNamedQuery12(name: String) =
    TypedProjectionQuery12<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12>(createTypedNamedQuery<Array<*>>(name))

fun <R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13> TypedEntityManager.createProjectionNamedQuery13(name: String) =
    TypedProjectionQuery13<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13>(
        createTypedNamedQuery<Array<*>>(name)
    )

fun <R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13, R14> TypedEntityManager.createProjectionNamedQuery14(
    name: String
) =
    TypedProjectionQuery14<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13, R14>(
        createTypedNamedQuery<Array<*>>(name)
    )
