package xyz.kotlinw.jpa.repository

import java.io.Serializable

interface AbstractEntityRepository<E: AbstractEntity<ID>, ID : Serializable>: GenericEntityRepository<E, ID>
