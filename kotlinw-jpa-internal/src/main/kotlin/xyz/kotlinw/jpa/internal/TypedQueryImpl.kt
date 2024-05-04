package xyz.kotlinw.jpa.internal

import jakarta.persistence.FlushModeType
import jakarta.persistence.LockModeType
import jakarta.persistence.Parameter
import jakarta.persistence.TemporalType
import java.util.*
import java.util.stream.Stream
import xyz.kotlinw.jpa.api.TypedQuery

@JvmInline
value class TypedQueryImpl<R : Any>(private val delegate: jakarta.persistence.TypedQuery<R>) :
    TypedQuery<R>, jakarta.persistence.TypedQuery<R> by delegate {

    override fun setHint(hintName: String, value: Any): TypedQuery<R> {
        delegate.setHint(hintName, value)
        return this
    }

    override fun <T : Any?> setParameter(param: Parameter<T>, value: T): TypedQuery<R> {
        delegate.setParameter(param, value)
        return this
    }

    override fun setParameter(
        param: Parameter<Calendar>,
        value: Calendar?,
        temporalType: TemporalType
    ): TypedQuery<R> {
        delegate.setParameter(param, value, temporalType)
        return this
    }

    override fun setParameter(param: Parameter<Date>, value: Date?, temporalType: TemporalType): TypedQuery<R> {
        delegate.setParameter(param, value, temporalType)
        return this
    }

    override fun setParameter(name: String, value: Any?): TypedQuery<R> {
        delegate.setParameter(name, value)
        return this
    }

    override fun setParameter(name: String, value: Calendar?, temporalType: TemporalType): TypedQuery<R> {
        delegate.setParameter(name, value, temporalType)
        return this
    }

    override fun setParameter(name: String, value: Date?, temporalType: TemporalType): TypedQuery<R> {
        delegate.setParameter(name, value, temporalType)
        return this
    }

    override fun setParameter(position: Int, value: Any?): TypedQuery<R> {
        delegate.setParameter(position, value)
        return this
    }

    override fun setParameter(position: Int, value: Calendar?, temporalType: TemporalType): TypedQuery<R> {
        delegate.setParameter(position, value, temporalType)
        return this
    }

    override fun setParameter(position: Int, value: Date?, temporalType: TemporalType): TypedQuery<R> {
        delegate.setParameter(position, value, temporalType)
        return this
    }

    override fun setFlushMode(flushMode: FlushModeType): TypedQuery<R> {
        delegate.setFlushMode(flushMode)
        return this
    }

    override fun setLockMode(lockMode: LockModeType): TypedQuery<R> {
        delegate.setLockMode(lockMode)
        return this
    }

    override fun <T : Any> unwrap(cls: Class<T>): T = delegate.unwrap(cls)

    override fun getHints(): Map<String, Any> = delegate.hints

    override fun getParameter(name: String): Parameter<*> = delegate.getParameter(name)

    override fun <T> getParameter(name: String, type: Class<T>): Parameter<T> = delegate.getParameter(name, type)

    override fun <T> getParameter(position: Int, type: Class<T>): Parameter<T> = delegate.getParameter(position, type)

    override fun isBound(param: Parameter<*>): Boolean = delegate.isBound(param)

    override fun <T> getParameterValue(param: Parameter<T>): T = delegate.getParameterValue(param)

    override fun getParameterValue(name: String): Any? = delegate.getParameterValue(name)

    override fun getParameterValue(position: Int): Any? = delegate.getParameterValue(position)

    override fun getParameter(position: Int): Parameter<*> = delegate.getParameter(position)

    override fun getParameters(): MutableSet<Parameter<*>> = delegate.parameters

    override fun getFlushMode(): FlushModeType = delegate.flushMode

    override fun getLockMode(): LockModeType = delegate.lockMode

    override fun getResultList(): List<R> = delegate.resultList

    override fun getResultStream(): Stream<R> = delegate.resultStream

    override fun getSingleResult(): R = delegate.singleResult

    override fun setMaxResults(maxResult: Int): TypedQuery<R> {
        delegate.setMaxResults(maxResult)
        return this
    }

    override fun setFirstResult(startPosition: Int): TypedQuery<R> {
        delegate.setFirstResult(startPosition)
        return this
    }
}
