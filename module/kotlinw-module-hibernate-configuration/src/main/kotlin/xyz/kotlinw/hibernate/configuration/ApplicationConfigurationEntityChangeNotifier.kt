package xyz.kotlinw.hibernate.configuration

import xyz.kotlinw.di.api.Component

sealed interface ApplicationConfigurationEntityChangeNotifier

internal interface ApplicationConfigurationEntityChangeNotifierImplementor :
    ApplicationConfigurationEntityChangeNotifier {

    fun notifyListener()

    fun addListener(listener: () -> Unit)
}

@Component
class ApplicationConfigurationEntityChangeNotifierImpl :
    ApplicationConfigurationEntityChangeNotifierImplementor {

    private lateinit var listener: () -> Unit

    override fun notifyListener() {
        listener()
    }

    override fun addListener(listener: () -> Unit) {
        this.listener = listener
    }
}
