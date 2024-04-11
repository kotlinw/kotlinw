package kotlinw.hibernate.core.api

sealed interface Transactional

object TransactionalImpl : Transactional {

    override fun toString() = Transactional::class.simpleName!!
}
