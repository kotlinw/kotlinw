package kotlinw.module.hibernate.core

import org.hibernate.SessionFactory

internal class SessionFactoryImpl(sessionFactory: SessionFactory) : SessionFactory by sessionFactory {
}
