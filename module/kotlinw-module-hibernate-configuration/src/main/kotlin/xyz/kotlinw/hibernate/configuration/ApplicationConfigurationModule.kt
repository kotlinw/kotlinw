package xyz.kotlinw.hibernate.configuration

import kotlinw.configuration.core.EnumerableConfigurationPropertyLookupSource
import kotlinw.configuration.core.EnumerableConfigurationPropertyLookupSourceImpl
import kotlinw.module.hibernate.core.BootstrapServiceRegistryCustomizer
import kotlinw.module.hibernate.core.HibernateModule
import org.hibernate.boot.Metadata
import org.hibernate.boot.spi.BootstrapContext
import org.hibernate.engine.spi.SessionFactoryImplementor
import org.hibernate.event.service.spi.EventListenerRegistry
import org.hibernate.event.spi.EventType
import org.hibernate.event.spi.PostDeleteEvent
import org.hibernate.event.spi.PostDeleteEventListener
import org.hibernate.event.spi.PostInsertEvent
import org.hibernate.event.spi.PostInsertEventListener
import org.hibernate.event.spi.PostUpdateEvent
import org.hibernate.event.spi.PostUpdateEventListener
import org.hibernate.integrator.spi.Integrator
import org.hibernate.persister.entity.EntityPersister
import org.hibernate.service.spi.SessionFactoryServiceRegistry
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.ComponentScan
import xyz.kotlinw.di.api.Module
import xyz.kotlinw.hibernate.configuration.entity.ApplicationConfigurationEntitiesModule
import xyz.kotlinw.hibernate.configuration.entity.ApplicationConfigurationEntity

@Module(includeModules = [HibernateModule::class, ApplicationConfigurationEntitiesModule::class])
@ComponentScan
class ApplicationConfigurationModule {

    @Component
    fun eventListenerRegistrar(
        notifier: ApplicationConfigurationEntityChangeNotifier
    ) =
        BootstrapServiceRegistryCustomizer {
            check(notifier is ApplicationConfigurationEntityChangeNotifierImplementor)
            applyIntegrator(
                object : Integrator {

                    // TODO legyen úgy megoldva az event listener regisztráció, ahogy a ReactiveQuerySupportServiceImpl is csinálja
                    override fun integrate(
                        metadata: Metadata,
                        bootstrapContext: BootstrapContext,
                        sessionFactory: SessionFactoryImplementor
                    ) {
                        val insertListener = object : PostInsertEventListener {

                            override fun requiresPostCommitHandling(persister: EntityPersister): Boolean = true

                            override fun onPostInsert(event: PostInsertEvent) {
                                if (event.persister.mappedClass == ApplicationConfigurationEntity::class.java) {
                                    notifier.notifyListener()
                                }
                            }
                        }

                        val updateListener = object : PostUpdateEventListener {

                            override fun requiresPostCommitHandling(persister: EntityPersister): Boolean = true

                            override fun onPostUpdate(event: PostUpdateEvent) {
                                if (event.persister.mappedClass == ApplicationConfigurationEntity::class.java) {
                                    notifier.notifyListener()
                                }
                            }
                        }

                        val deleteListener = object : PostDeleteEventListener {
                            override fun requiresPostCommitHandling(persister: EntityPersister): Boolean = true

                            override fun onPostDelete(event: PostDeleteEvent) {
                                if (event.persister.mappedClass == ApplicationConfigurationEntity::class.java) {
                                    notifier.notifyListener()
                                }
                            }
                        }

                        val eventListenerRegistry =
                            sessionFactory.serviceRegistry.getService(EventListenerRegistry::class.java)
                                ?: throw IllegalStateException()
                        eventListenerRegistry.appendListeners(EventType.POST_COMMIT_INSERT, insertListener)
                        eventListenerRegistry.appendListeners(EventType.POST_COMMIT_UPDATE, updateListener)
                        eventListenerRegistry.appendListeners(EventType.POST_COMMIT_DELETE, deleteListener)
                    }

                    override fun disintegrate(
                        sessionFactory: SessionFactoryImplementor,
                        sessionFactoryServiceRegistry: SessionFactoryServiceRegistry
                    ) {
                    }
                }
            )
        }

    @Component
    fun databaseConfigurationEntityPropertyLookupSource(
        applicationConfigurationEntityConfigurationPropertyResolver: ApplicationConfigurationEntityConfigurationPropertyResolver
    ): EnumerableConfigurationPropertyLookupSource =
        EnumerableConfigurationPropertyLookupSourceImpl(applicationConfigurationEntityConfigurationPropertyResolver)
}
