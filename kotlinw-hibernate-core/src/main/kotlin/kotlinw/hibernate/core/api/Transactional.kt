package kotlinw.hibernate.core.api

sealed interface Transactional

data object TransactionalImpl : Transactional
