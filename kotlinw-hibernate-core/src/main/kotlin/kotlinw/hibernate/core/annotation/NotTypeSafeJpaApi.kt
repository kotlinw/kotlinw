package kotlinw.hibernate.core.annotation

@MustBeDocumented
@Retention(value = AnnotationRetention.BINARY)
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This API is not type-safe, a type-safe alternative should be used instead."
)
annotation class NotTypeSafeJpaApi
