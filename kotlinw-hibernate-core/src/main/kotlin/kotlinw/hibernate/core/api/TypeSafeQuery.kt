package kotlinw.hibernate.core.api

import jakarta.persistence.FlushModeType
import jakarta.persistence.LockModeType
import jakarta.persistence.Parameter
import jakarta.persistence.TemporalType
import jakarta.persistence.TypedQuery
import java.util.*
import java.util.stream.Stream

interface TypeSafeQuery<R> : TypedQuery<R> {

    override fun setHint(hintName: String, value: Any): TypeSafeQuery<R>

    override fun <T : Any?> setParameter(param: Parameter<T>, value: T): TypeSafeQuery<R>

    override fun setParameter(
        param: Parameter<Calendar>,
        value: Calendar?,
        temporalType: TemporalType
    ): TypeSafeQuery<R>

    override fun setParameter(param: Parameter<Date>, value: Date?, temporalType: TemporalType): TypeSafeQuery<R>

    override fun setParameter(name: String, value: Any?): TypeSafeQuery<R>

    override fun setParameter(name: String, value: Calendar?, temporalType: TemporalType): TypeSafeQuery<R>

    override fun setParameter(name: String, value: Date?, temporalType: TemporalType): TypeSafeQuery<R>

    override fun setParameter(position: Int, value: Any?): TypeSafeQuery<R>

    override fun setParameter(position: Int, value: Calendar?, temporalType: TemporalType): TypeSafeQuery<R>

    override fun setParameter(position: Int, value: Date?, temporalType: TemporalType): TypeSafeQuery<R>

    override fun setFlushMode(flushMode: FlushModeType): TypeSafeQuery<R>

    override fun setLockMode(lockMode: LockModeType): TypeSafeQuery<R>

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

    override fun setMaxResults(maxResult: Int): TypeSafeQuery<R>

    override fun setFirstResult(startPosition: Int): TypeSafeQuery<R>
}

inline fun <reified R> TypeSafeEntityManager.createTypeSafeQuery(qlString: String) =
    TypeSafeQueryImpl(createQuery(qlString, R::class.java))

@JvmInline
value class TypeSafeQueryImpl<R>(private val query: TypedQuery<R>) : TypeSafeQuery<R>, TypedQuery<R> by query {

    override fun setHint(hintName: String, value: Any): TypeSafeQuery<R> =
        TypeSafeQueryImpl(query.setHint(hintName, value))

    override fun <T : Any?> setParameter(param: Parameter<T>, value: T): TypeSafeQuery<R> =
        TypeSafeQueryImpl(query.setParameter(param, value))

    override fun setParameter(
        param: Parameter<Calendar>,
        value: Calendar?,
        temporalType: TemporalType
    ): TypeSafeQuery<R> =
        TypeSafeQueryImpl(query.setParameter(param, value, temporalType))

    override fun setParameter(param: Parameter<Date>, value: Date?, temporalType: TemporalType): TypeSafeQuery<R> =
        TypeSafeQueryImpl(query.setParameter(param, value, temporalType))

    override fun setParameter(name: String, value: Any?): TypeSafeQuery<R> =
        TypeSafeQueryImpl(query.setParameter(name, value))

    override fun setParameter(name: String, value: Calendar?, temporalType: TemporalType): TypeSafeQuery<R> =
        TypeSafeQueryImpl(query.setParameter(name, value, temporalType))

    override fun setParameter(name: String, value: Date?, temporalType: TemporalType): TypeSafeQuery<R> =
        TypeSafeQueryImpl(query.setParameter(name, value, temporalType))

    override fun setParameter(position: Int, value: Any?): TypeSafeQuery<R> =
        TypeSafeQueryImpl(query.setParameter(position, value))

    override fun setParameter(position: Int, value: Calendar?, temporalType: TemporalType): TypeSafeQuery<R> =
        TypeSafeQueryImpl(query.setParameter(position, value, temporalType))

    override fun setParameter(position: Int, value: Date?, temporalType: TemporalType): TypeSafeQuery<R> =
        TypeSafeQueryImpl(query.setParameter(position, value, temporalType))

    override fun setFlushMode(flushMode: FlushModeType): TypeSafeQuery<R> =
        TypeSafeQueryImpl(query.setFlushMode(flushMode))

    override fun setLockMode(lockMode: LockModeType): TypeSafeQuery<R> =
        TypeSafeQueryImpl(query.setLockMode(lockMode))

    override fun <T : Any> unwrap(cls: Class<T>): T = query.unwrap(cls)

    override fun getHints(): Map<String, Any> = query.hints

    override fun getParameter(name: String): Parameter<*> = query.getParameter(name)

    override fun <T> getParameter(name: String, type: Class<T>): Parameter<T> = query.getParameter(name, type)

    override fun <T> getParameter(position: Int, type: Class<T>): Parameter<T> = query.getParameter(position, type)

    override fun isBound(param: Parameter<*>): Boolean = query.isBound(param)

    override fun <T> getParameterValue(param: Parameter<T>): T = query.getParameterValue(param)

    override fun getParameterValue(name: String): Any? = query.getParameterValue(name)

    override fun getParameterValue(position: Int): Any? = query.getParameterValue(position)

    override fun getParameter(position: Int): Parameter<*> = query.getParameter(position)

    override fun getParameters(): MutableSet<Parameter<*>> = query.parameters

    override fun getFlushMode(): FlushModeType = query.flushMode

    override fun getLockMode(): LockModeType = query.lockMode

    override fun getResultList(): List<R> = query.resultList

    override fun getResultStream(): Stream<R> = query.resultStream

    override fun getSingleResult(): R = query.singleResult

    override fun setMaxResults(maxResult: Int): TypeSafeQuery<R> =
        TypeSafeQueryImpl(query.setMaxResults(maxResult))

    override fun setFirstResult(startPosition: Int): TypeSafeQuery<R> =
        TypeSafeQueryImpl(query.setFirstResult(startPosition))
}
