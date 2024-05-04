package xyz.kotlinw.jpa.core

import xyz.kotlinw.jpa.api.TypeSafeEntityManager
import xyz.kotlinw.jpa.api.TypeSafeProjectionQuery10
import xyz.kotlinw.jpa.api.TypeSafeProjectionQuery11
import xyz.kotlinw.jpa.api.TypeSafeProjectionQuery12
import xyz.kotlinw.jpa.api.TypeSafeProjectionQuery13
import xyz.kotlinw.jpa.api.TypeSafeProjectionQuery14
import xyz.kotlinw.jpa.api.TypeSafeProjectionQuery2
import xyz.kotlinw.jpa.api.TypeSafeProjectionQuery3
import xyz.kotlinw.jpa.api.TypeSafeProjectionQuery4
import xyz.kotlinw.jpa.api.TypeSafeProjectionQuery5
import xyz.kotlinw.jpa.api.TypeSafeProjectionQuery6
import xyz.kotlinw.jpa.api.TypeSafeProjectionQuery7
import xyz.kotlinw.jpa.api.TypeSafeProjectionQuery8
import xyz.kotlinw.jpa.api.TypeSafeProjectionQuery9

//
// Projection query (query string)
//

fun <R1, R2> TypeSafeEntityManager.createProjectionQuery2(qlString: String) =
    TypeSafeProjectionQuery2<R1, R2>(createTypeSafeQuery<Array<*>>(qlString))

fun <R1, R2, R3> TypeSafeEntityManager.createProjectionQuery3(qlString: String) =
    TypeSafeProjectionQuery3<R1, R2, R3>(createTypeSafeQuery<Array<*>>(qlString))

fun <R1, R2, R3, R4> TypeSafeEntityManager.createProjectionQuery4(qlString: String) =
    TypeSafeProjectionQuery4<R1, R2, R3, R4>(createTypeSafeQuery<Array<*>>(qlString))

fun <R1, R2, R3, R4, R5> TypeSafeEntityManager.createProjectionQuery5(qlString: String) =
    TypeSafeProjectionQuery5<R1, R2, R3, R4, R5>(createTypeSafeQuery<Array<*>>(qlString))

fun <R1, R2, R3, R4, R5, R6> TypeSafeEntityManager.createProjectionQuery6(qlString: String) =
    TypeSafeProjectionQuery6<R1, R2, R3, R4, R5, R6>(createTypeSafeQuery<Array<*>>(qlString))

fun <R1, R2, R3, R4, R5, R6, R7> TypeSafeEntityManager.createProjectionQuery7(qlString: String) =
    TypeSafeProjectionQuery7<R1, R2, R3, R4, R5, R6, R7>(createTypeSafeQuery<Array<*>>(qlString))

fun <R1, R2, R3, R4, R5, R6, R7, R8> TypeSafeEntityManager.createProjectionQuery8(qlString: String) =
    TypeSafeProjectionQuery8<R1, R2, R3, R4, R5, R6, R7, R8>(createTypeSafeQuery<Array<*>>(qlString))

fun <R1, R2, R3, R4, R5, R6, R7, R8, R9> TypeSafeEntityManager.createProjectionQuery9(qlString: String) =
    TypeSafeProjectionQuery9<R1, R2, R3, R4, R5, R6, R7, R8, R9>(createTypeSafeQuery<Array<*>>(qlString))

fun <R1, R2, R3, R4, R5, R6, R7, R8, R9, R10> TypeSafeEntityManager.createProjectionQuery10(qlString: String) =
    TypeSafeProjectionQuery10<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10>(createTypeSafeQuery<Array<*>>(qlString))

fun <R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11> TypeSafeEntityManager.createProjectionQuery11(qlString: String) =
    TypeSafeProjectionQuery11<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11>(createTypeSafeQuery<Array<*>>(qlString))

fun <R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12> TypeSafeEntityManager.createProjectionQuery12(qlString: String) =
    TypeSafeProjectionQuery12<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12>(createTypeSafeQuery<Array<*>>(qlString))

fun <R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13> TypeSafeEntityManager.createProjectionQuery13(qlString: String) =
    TypeSafeProjectionQuery13<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13>(
        createTypeSafeQuery<Array<*>>(qlString)
    )

fun <R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13, R14> TypeSafeEntityManager.createProjectionQuery14(qlString: String) =
    TypeSafeProjectionQuery14<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13, R14>(
        createTypeSafeQuery<Array<*>>(qlString)
    )

//
// Projection query (named query)
//

fun <R1, R2> TypeSafeEntityManager.createProjectionNamedQuery2(name: String) =
    TypeSafeProjectionQuery2<R1, R2>(createTypeSafeNamedQuery<Array<*>>(name))

fun <R1, R2, R3> TypeSafeEntityManager.createProjectionNamedQuery3(name: String) =
    TypeSafeProjectionQuery3<R1, R2, R3>(createTypeSafeNamedQuery<Array<*>>(name))

fun <R1, R2, R3, R4> TypeSafeEntityManager.createProjectionNamedQuery4(name: String) =
    TypeSafeProjectionQuery4<R1, R2, R3, R4>(createTypeSafeNamedQuery<Array<*>>(name))

fun <R1, R2, R3, R4, R5> TypeSafeEntityManager.createProjectionNamedQuery5(name: String) =
    TypeSafeProjectionQuery5<R1, R2, R3, R4, R5>(createTypeSafeNamedQuery<Array<*>>(name))

fun <R1, R2, R3, R4, R5, R6> TypeSafeEntityManager.createProjectionNamedQuery6(name: String) =
    TypeSafeProjectionQuery6<R1, R2, R3, R4, R5, R6>(createTypeSafeNamedQuery<Array<*>>(name))

fun <R1, R2, R3, R4, R5, R6, R7> TypeSafeEntityManager.createProjectionNamedQuery7(name: String) =
    TypeSafeProjectionQuery7<R1, R2, R3, R4, R5, R6, R7>(createTypeSafeNamedQuery<Array<*>>(name))

fun <R1, R2, R3, R4, R5, R6, R7, R8> TypeSafeEntityManager.createProjectionNamedQuery8(name: String) =
    TypeSafeProjectionQuery8<R1, R2, R3, R4, R5, R6, R7, R8>(createTypeSafeNamedQuery<Array<*>>(name))

fun <R1, R2, R3, R4, R5, R6, R7, R8, R9> TypeSafeEntityManager.createProjectionNamedQuery9(name: String) =
    TypeSafeProjectionQuery9<R1, R2, R3, R4, R5, R6, R7, R8, R9>(createTypeSafeNamedQuery<Array<*>>(name))

fun <R1, R2, R3, R4, R5, R6, R7, R8, R9, R10> TypeSafeEntityManager.createProjectionNamedQuery10(name: String) =
    TypeSafeProjectionQuery10<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10>(createTypeSafeNamedQuery<Array<*>>(name))

fun <R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11> TypeSafeEntityManager.createProjectionNamedQuery11(name: String) =
    TypeSafeProjectionQuery11<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11>(createTypeSafeNamedQuery<Array<*>>(name))

fun <R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12> TypeSafeEntityManager.createProjectionNamedQuery12(name: String) =
    TypeSafeProjectionQuery12<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12>(createTypeSafeNamedQuery<Array<*>>(name))

fun <R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13> TypeSafeEntityManager.createProjectionNamedQuery13(name: String) =
    TypeSafeProjectionQuery13<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13>(
        createTypeSafeNamedQuery<Array<*>>(name)
    )

fun <R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13, R14> TypeSafeEntityManager.createProjectionNamedQuery14(
    name: String
) =
    TypeSafeProjectionQuery14<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13, R14>(
        createTypeSafeNamedQuery<Array<*>>(name)
    )
