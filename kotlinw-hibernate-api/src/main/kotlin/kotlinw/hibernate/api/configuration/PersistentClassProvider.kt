package kotlinw.hibernate.api.configuration

import kotlin.reflect.KClass

// TODO ez modul specifikus!
fun interface PersistentClassProvider {

    fun getPersistentClasses(): List<KClass<*>>
}
