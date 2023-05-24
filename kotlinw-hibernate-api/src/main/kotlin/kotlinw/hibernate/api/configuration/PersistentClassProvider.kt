package kotlinw.hibernate.api.configuration

import kotlin.reflect.KClass

fun interface PersistentClassProvider {

    fun getPersistentClasses(): List<KClass<*>>
}
