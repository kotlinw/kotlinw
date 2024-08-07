package xyz.kotlinw.jpa.api

import jakarta.persistence.FlushModeType
import jakarta.persistence.LockModeType
import jakarta.persistence.NoResultException
import jakarta.persistence.Parameter
import jakarta.persistence.TemporalType
import java.util.*
import java.util.stream.Stream
import jakarta.persistence.TypedQuery as JpaTypedQuery

interface TypedQuery<R : Any> : JpaTypedQuery<R> {

    override fun setHint(hintName: String, value: Any): TypedQuery<R>

    override fun <T : Any?> setParameter(param: Parameter<T>, value: T): TypedQuery<R>

    override fun setParameter(
        param: Parameter<Calendar>,
        value: Calendar?,
        temporalType: TemporalType
    ): TypedQuery<R>

    override fun setParameter(param: Parameter<Date>, value: Date?, temporalType: TemporalType): TypedQuery<R>

    override fun setParameter(name: String, value: Any?): TypedQuery<R>

    override fun setParameter(name: String, value: Calendar?, temporalType: TemporalType): TypedQuery<R>

    override fun setParameter(name: String, value: Date?, temporalType: TemporalType): TypedQuery<R>

    override fun setParameter(position: Int, value: Any?): TypedQuery<R>

    override fun setParameter(position: Int, value: Calendar?, temporalType: TemporalType): TypedQuery<R>

    override fun setParameter(position: Int, value: Date?, temporalType: TemporalType): TypedQuery<R>

    override fun setFlushMode(flushMode: FlushModeType): TypedQuery<R>

    override fun setLockMode(lockMode: LockModeType): TypedQuery<R>

    override fun <T : Any> unwrap(cls: Class<T>): T

    override fun getHints(): Map<String, Any>

    override fun getParameter(name: String): Parameter<*>

    override fun <T> getParameter(name: String, type: Class<T>): Parameter<T>

    override fun <T> getParameter(position: Int, type: Class<T>): Parameter<T>

    override fun isBound(param: Parameter<*>): Boolean

    override fun <T> getParameterValue(param: Parameter<T>): T

    override fun getParameterValue(name: String): Any?

    override fun getParameterValue(position: Int): Any?

    override fun getParameter(position: Int): Parameter<*>

    override fun getParameters(): MutableSet<Parameter<*>>

    override fun getFlushMode(): FlushModeType

    override fun getLockMode(): LockModeType

    override fun getResultList(): List<R>

    override fun getResultStream(): Stream<R>

    override fun getSingleResult(): R

    override fun setMaxResults(maxResult: Int): TypedQuery<R>

    override fun setFirstResult(startPosition: Int): TypedQuery<R>

    override fun executeUpdate(): Int

    override fun getMaxResults(): Int

    override fun getFirstResult(): Int
}
