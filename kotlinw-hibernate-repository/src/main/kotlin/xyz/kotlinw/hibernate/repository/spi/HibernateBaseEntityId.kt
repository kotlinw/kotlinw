package xyz.kotlinw.hibernate.repository.spi

import kotlin.annotation.AnnotationRetention.RUNTIME
import org.hibernate.annotations.IdGeneratorType


@IdGeneratorType(HibernateBaseEntityIdGenerator::class)
@Retention(RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER)
annotation class HibernateBaseEntityId(
    val sequenceName: String = "HibernateBaseEntitySequence",
    val startWith: Int = 1,
    val incrementBy: Int = 50
)
