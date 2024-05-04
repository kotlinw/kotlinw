package xyz.kotlinw.jpa.api

import arrow.core.Tuple10
import arrow.core.Tuple11
import arrow.core.Tuple12
import arrow.core.Tuple13
import arrow.core.Tuple14
import arrow.core.Tuple4
import arrow.core.Tuple5
import arrow.core.Tuple6
import arrow.core.Tuple7
import arrow.core.Tuple8
import arrow.core.Tuple9
import jakarta.persistence.FlushModeType
import jakarta.persistence.LockModeType
import jakarta.persistence.Parameter
import jakarta.persistence.TemporalType
import java.util.*
import java.util.stream.Stream

private fun checkResultRow(row: Any?, expectedSize: Int): Array<*> {
    check(row is Array<*>) { "Invalid projection query result row, expected an array but got: $row" }
    check(row.size == expectedSize) { "Projection query result row size mismatch, expected size of $expectedSize but got ${row.size}: $row" }
    return row
}

private fun <R> mapResultElement(row: Array<*>, index: Int): R =
    try {
        @Suppress("UNCHECKED_CAST")
        row[index] as R
    } catch (e: ClassCastException) {
        throw IllegalStateException("Invalid result element type: ${row[index]}")
    }

interface TypedProjectionQuery<R : Any> : TypedQuery<R> {

    override fun setHint(hintName: String, value: Any): TypedProjectionQuery<R>

    override fun <T : Any?> setParameter(param: Parameter<T>, value: T): TypedProjectionQuery<R>

    override fun setParameter(
        param: Parameter<Calendar>,
        value: Calendar?,
        temporalType: TemporalType
    ): TypedProjectionQuery<R>

    override fun setParameter(
        param: Parameter<Date>,
        value: Date?,
        temporalType: TemporalType
    ): TypedProjectionQuery<R>

    override fun setParameter(name: String, value: Any?): TypedProjectionQuery<R>

    override fun setParameter(name: String, value: Calendar?, temporalType: TemporalType): TypedProjectionQuery<R>

    override fun setParameter(name: String, value: Date?, temporalType: TemporalType): TypedProjectionQuery<R>

    override fun setParameter(position: Int, value: Any?): TypedProjectionQuery<R>

    override fun setParameter(position: Int, value: Calendar?, temporalType: TemporalType): TypedProjectionQuery<R>

    override fun setParameter(position: Int, value: Date?, temporalType: TemporalType): TypedProjectionQuery<R>

    override fun setFlushMode(flushMode: FlushModeType): TypedProjectionQuery<R>

    override fun setLockMode(lockMode: LockModeType): TypedProjectionQuery<R>
}

sealed class AbstractTypedProjectionQuery<R : Any, Q : AbstractTypedProjectionQuery<R, Q>>(
    private val query: TypedQuery<Array<*>>,
    private val tupleSize: Int
) : TypedProjectionQuery<R> {

    protected abstract fun mapRow(it: Array<*>): R

    @Suppress("UNCHECKED_CAST")
    private fun self(): Q = this as Q

    final override fun getResultList(): List<R> =
        query.resultList
            .map { checkResultRow(it, tupleSize) }
            .map { mapRow(it) }

    final override fun getSingleResult(): R =
        mapRow(checkResultRow(query.singleResult, tupleSize))

    final override fun getResultStream(): Stream<R> =
        query.resultStream
            .map { checkResultRow(it, tupleSize) }
            .map { mapRow(it) }

    override fun setMaxResults(maxResult: Int): Q =
        query.setMaxResults(maxResult).let { self() }

    override fun setFirstResult(startPosition: Int): Q =
        query.setFirstResult(startPosition).let { self() }

    override fun setHint(hintName: String, value: Any): Q =
        query.setHint(hintName, value).let { self() }

    override fun setParameter(
        position: Int,
        value: Calendar?,
        temporalType: TemporalType
    ): Q =
        query.setParameter(position, value, temporalType).let { self() }

    override fun setParameter(
        param: Parameter<Calendar>,
        value: Calendar?,
        temporalType: TemporalType
    ): Q =
        query.setParameter(param, value, temporalType).let { self() }

    override fun setParameter(name: String, value: Any?): Q =
        query.setParameter(name, value).let { self() }

    override fun setParameter(
        name: String,
        value: Calendar?,
        temporalType: TemporalType
    ): Q =
        query.setParameter(name, value, temporalType).let { self() }

    override fun <T : Any?> setParameter(param: Parameter<T>, value: T): Q =
        query.setParameter(param, value).let { self() }

    override fun setParameter(
        name: String,
        value: Date?,
        temporalType: TemporalType
    ): Q =
        query.setParameter(name, value, temporalType).let { self() }

    override fun setParameter(
        param: Parameter<Date>,
        value: Date?,
        temporalType: TemporalType
    ): Q =
        query.setParameter(param, value, temporalType).let { self() }

    override fun setParameter(position: Int, value: Any?): Q =
        query.setParameter(position, value).let { self() }

    override fun setParameter(
        position: Int,
        value: Date?,
        temporalType: TemporalType
    ): Q =
        query.setParameter(position, value, temporalType).let { self() }

    override fun setFlushMode(flushMode: FlushModeType): Q =
        query.setFlushMode(flushMode).let { self() }

    override fun setLockMode(lockMode: LockModeType): Q =
        query.setLockMode(lockMode).let { self() }

    override fun <T : Any> unwrap(cls: Class<T>): T = query.unwrap(cls)

    override fun getHints(): Map<String, Any> = query.hints

    override fun getParameter(name: String): Parameter<*> = query.getParameter(name)

    override fun <T> getParameter(name: String, type: Class<T>): Parameter<T> = query.getParameter(name, type)

    override fun <T> getParameter(position: Int, type: Class<T>): Parameter<T> = query.getParameter(position, type)

    override fun getParameter(position: Int): Parameter<*> = query.getParameter(position)

    override fun isBound(param: Parameter<*>): Boolean = query.isBound(param)

    override fun <T> getParameterValue(param: Parameter<T>): T = query.getParameterValue(param)

    override fun getParameterValue(name: String): Any? = query.getParameterValue(name)

    override fun getParameterValue(position: Int): Any? = query.getParameterValue(position)

    override fun getParameters(): MutableSet<Parameter<*>> = query.getParameters()

    override fun getFlushMode(): FlushModeType = query.getFlushMode()

    override fun getLockMode(): LockModeType = query.getLockMode()

    override fun executeUpdate(): Int = query.executeUpdate()

    override fun getMaxResults(): Int = query.maxResults

    override fun getFirstResult(): Int = query.firstResult
}

class TypedProjectionQuery2<R1, R2>(query: TypedQuery<Array<*>>) :
    AbstractTypedProjectionQuery<Pair<R1, R2>,
            TypedProjectionQuery2<R1, R2>>(query, 2) {

    override fun mapRow(it: Array<*>) =
        Pair(mapResultElement<R1>(it, 0), mapResultElement<R2>(it, 1))
}

class TypedProjectionQuery3<R1, R2, R3>(query: TypedQuery<Array<*>>) :
    AbstractTypedProjectionQuery<Triple<R1, R2, R3>,
            TypedProjectionQuery3<R1, R2, R3>>(query, 3) {

    override fun mapRow(it: Array<*>) =
        Triple(mapResultElement<R1>(it, 0), mapResultElement<R2>(it, 1), mapResultElement<R3>(it, 2))
}

class TypedProjectionQuery4<R1, R2, R3, R4>(query: TypedQuery<Array<*>>) :
    AbstractTypedProjectionQuery<Tuple4<R1, R2, R3, R4>,
            TypedProjectionQuery4<R1, R2, R3, R4>>(query, 4) {

    override fun mapRow(it: Array<*>) =
        Tuple4(
            mapResultElement<R1>(it, 0),
            mapResultElement<R2>(it, 1),
            mapResultElement<R3>(it, 2),
            mapResultElement<R4>(it, 3)
        )
}

class TypedProjectionQuery5<R1, R2, R3, R4, R5>(query: TypedQuery<Array<*>>) :
    AbstractTypedProjectionQuery<Tuple5<R1, R2, R3, R4, R5>,
            TypedProjectionQuery5<R1, R2, R3, R4, R5>>(query, 5) {

    override fun mapRow(it: Array<*>) =
        Tuple5(
            mapResultElement<R1>(it, 0),
            mapResultElement<R2>(it, 1),
            mapResultElement<R3>(it, 2),
            mapResultElement<R4>(it, 3),
            mapResultElement<R5>(it, 4)
        )
}

class TypedProjectionQuery6<R1, R2, R3, R4, R5, R6>(query: TypedQuery<Array<*>>) :
    AbstractTypedProjectionQuery<Tuple6<R1, R2, R3, R4, R5, R6>,
            TypedProjectionQuery6<R1, R2, R3, R4, R5, R6>>(query, 6) {

    override fun mapRow(it: Array<*>) =
        Tuple6(
            mapResultElement<R1>(it, 0),
            mapResultElement<R2>(it, 1),
            mapResultElement<R3>(it, 2),
            mapResultElement<R4>(it, 3),
            mapResultElement<R5>(it, 4),
            mapResultElement<R6>(it, 5)
        )
}

class TypedProjectionQuery7<R1, R2, R3, R4, R5, R6, R7>(query: TypedQuery<Array<*>>) :
    AbstractTypedProjectionQuery<Tuple7<R1, R2, R3, R4, R5, R6, R7>,
            TypedProjectionQuery7<R1, R2, R3, R4, R5, R6, R7>>(query, 7) {

    override fun mapRow(it: Array<*>) =
        Tuple7(
            mapResultElement<R1>(it, 0),
            mapResultElement<R2>(it, 1),
            mapResultElement<R3>(it, 2),
            mapResultElement<R4>(it, 3),
            mapResultElement<R5>(it, 4),
            mapResultElement<R6>(it, 5),
            mapResultElement<R7>(it, 6)
        )
}

class TypedProjectionQuery8<R1, R2, R3, R4, R5, R6, R7, R8>(query: TypedQuery<Array<*>>) :
    AbstractTypedProjectionQuery<Tuple8<R1, R2, R3, R4, R5, R6, R7, R8>,
            TypedProjectionQuery8<R1, R2, R3, R4, R5, R6, R7, R8>>(query, 8) {

    override fun mapRow(it: Array<*>) =
        Tuple8(
            mapResultElement<R1>(it, 0),
            mapResultElement<R2>(it, 1),
            mapResultElement<R3>(it, 2),
            mapResultElement<R4>(it, 3),
            mapResultElement<R5>(it, 4),
            mapResultElement<R6>(it, 5),
            mapResultElement<R7>(it, 6),
            mapResultElement<R8>(it, 7)
        )
}

class TypedProjectionQuery9<R1, R2, R3, R4, R5, R6, R7, R8, R9>(query: TypedQuery<Array<*>>) :
    AbstractTypedProjectionQuery<Tuple9<R1, R2, R3, R4, R5, R6, R7, R8, R9>,
            TypedProjectionQuery9<R1, R2, R3, R4, R5, R6, R7, R8, R9>>(query, 9) {

    override fun mapRow(it: Array<*>) =
        Tuple9(
            mapResultElement<R1>(it, 0),
            mapResultElement<R2>(it, 1),
            mapResultElement<R3>(it, 2),
            mapResultElement<R4>(it, 3),
            mapResultElement<R5>(it, 4),
            mapResultElement<R6>(it, 5),
            mapResultElement<R7>(it, 6),
            mapResultElement<R8>(it, 7),
            mapResultElement<R9>(it, 8)
        )
}

class TypedProjectionQuery10<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10>(query: TypedQuery<Array<*>>) :
    AbstractTypedProjectionQuery<Tuple10<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10>,
            TypedProjectionQuery10<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10>>(query, 10) {

    override fun mapRow(it: Array<*>) =
        Tuple10(
            mapResultElement<R1>(it, 0),
            mapResultElement<R2>(it, 1),
            mapResultElement<R3>(it, 2),
            mapResultElement<R4>(it, 3),
            mapResultElement<R5>(it, 4),
            mapResultElement<R6>(it, 5),
            mapResultElement<R7>(it, 6),
            mapResultElement<R8>(it, 7),
            mapResultElement<R9>(it, 8),
            mapResultElement<R10>(it, 9)
        )
}

class TypedProjectionQuery11<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11>(query: TypedQuery<Array<*>>) :
    AbstractTypedProjectionQuery<Tuple11<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11>,
            TypedProjectionQuery11<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11>>(query, 11) {

    override fun mapRow(it: Array<*>) =
        Tuple11(
            mapResultElement<R1>(it, 0),
            mapResultElement<R2>(it, 1),
            mapResultElement<R3>(it, 2),
            mapResultElement<R4>(it, 3),
            mapResultElement<R5>(it, 4),
            mapResultElement<R6>(it, 5),
            mapResultElement<R7>(it, 6),
            mapResultElement<R8>(it, 7),
            mapResultElement<R9>(it, 8),
            mapResultElement<R10>(it, 9),
            mapResultElement<R11>(it, 10)
        )
}

class TypedProjectionQuery12<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12>(query: TypedQuery<Array<*>>) :
    AbstractTypedProjectionQuery<Tuple12<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12>,
            TypedProjectionQuery12<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12>>(query, 12) {

    override fun mapRow(it: Array<*>) =
        Tuple12(
            mapResultElement<R1>(it, 0),
            mapResultElement<R2>(it, 1),
            mapResultElement<R3>(it, 2),
            mapResultElement<R4>(it, 3),
            mapResultElement<R5>(it, 4),
            mapResultElement<R6>(it, 5),
            mapResultElement<R7>(it, 6),
            mapResultElement<R8>(it, 7),
            mapResultElement<R9>(it, 8),
            mapResultElement<R10>(it, 9),
            mapResultElement<R11>(it, 10),
            mapResultElement<R12>(it, 11)
        )
}

class TypedProjectionQuery13<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13>(query: TypedQuery<Array<*>>) :
    AbstractTypedProjectionQuery<Tuple13<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13>,
            TypedProjectionQuery13<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13>>(query, 13) {

    override fun mapRow(it: Array<*>) =
        Tuple13(
            mapResultElement<R1>(it, 0),
            mapResultElement<R2>(it, 1),
            mapResultElement<R3>(it, 2),
            mapResultElement<R4>(it, 3),
            mapResultElement<R5>(it, 4),
            mapResultElement<R6>(it, 5),
            mapResultElement<R7>(it, 6),
            mapResultElement<R8>(it, 7),
            mapResultElement<R9>(it, 8),
            mapResultElement<R10>(it, 9),
            mapResultElement<R11>(it, 10),
            mapResultElement<R12>(it, 11),
            mapResultElement<R13>(it, 12)
        )
}

class TypedProjectionQuery14<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13, R14>(query: TypedQuery<Array<*>>) :
    AbstractTypedProjectionQuery<Tuple14<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13, R14>,
            TypedProjectionQuery14<R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, R12, R13, R14>>(query, 14) {

    override fun mapRow(it: Array<*>) =
        Tuple14(
            mapResultElement<R1>(it, 0),
            mapResultElement<R2>(it, 1),
            mapResultElement<R3>(it, 2),
            mapResultElement<R4>(it, 3),
            mapResultElement<R5>(it, 4),
            mapResultElement<R6>(it, 5),
            mapResultElement<R7>(it, 6),
            mapResultElement<R8>(it, 7),
            mapResultElement<R9>(it, 8),
            mapResultElement<R10>(it, 9),
            mapResultElement<R11>(it, 10),
            mapResultElement<R12>(it, 11),
            mapResultElement<R13>(it, 12),
            mapResultElement<R14>(it, 13),
        )
}

// TODO ... Tuple22
