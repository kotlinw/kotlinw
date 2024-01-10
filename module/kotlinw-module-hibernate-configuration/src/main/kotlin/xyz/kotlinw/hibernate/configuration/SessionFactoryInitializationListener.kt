package xyz.kotlinw.hibernate.configuration

import org.hibernate.SessionFactory

sealed interface SessionFactoryInitializationListener

internal class SessionFactoryInitializationListenerImpl(
    sessionFactory: SessionFactory,

): SessionFactoryInitializationListener {

}
