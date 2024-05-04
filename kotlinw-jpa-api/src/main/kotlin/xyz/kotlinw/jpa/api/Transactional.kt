package xyz.kotlinw.jpa.api

sealed interface Transactional

data object TransactionalImpl : Transactional
