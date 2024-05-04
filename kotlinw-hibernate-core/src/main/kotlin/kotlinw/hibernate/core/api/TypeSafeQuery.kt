package kotlinw.hibernate.core.api

import jakarta.persistence.FlushModeType
import jakarta.persistence.LockModeType
import jakarta.persistence.NoResultException
import jakarta.persistence.Parameter
import jakarta.persistence.TemporalType
import jakarta.persistence.TypedQuery
import kotlin.reflect.KClass
import java.util.*
import java.util.stream.Stream

interface TypeSafeQuery<R : Any> : TypedQuery<R> {

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

fun <R : Any> TypeSafeQuery<R>.getSingleResultOrNull(): R? =
    try {
        getSingleResult()
    } catch (e: NoResultException) {
        null
    }

fun <R : Any> TypeSafeEntityManager.createTypeSafeQuery(
    qlString: String,
    resultType: KClass<R>,
    vararg arguments: Any?
): TypeSafeQuery<R> =
    TypeSafeQueryImpl(createQuery(qlString, resultType.java)).also {
        arguments.forEachIndexed { index, value ->
            it.setParameter(index + 1, value)
        }
    }

inline fun <reified R : Any> TypeSafeEntityManager.createTypeSafeQuery(qlString: String, vararg arguments: Any?) =
    createTypeSafeQuery(qlString, R::class, *arguments)

fun <R : Any> TypeSafeEntityManager.query(
    qlString: String,
    resultType: KClass<R>,
    vararg arguments: Any?
): List<R> =
    createTypeSafeQuery(qlString, resultType, *arguments).resultList

inline fun <reified R : Any> TypeSafeEntityManager.query(
    qlString: String,
    vararg arguments: Any?
): List<R> =
    query(qlString, R::class, *arguments)
