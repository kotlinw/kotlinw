package kotlinw.module.hibernate.core

import kotlin.reflect.KClass
import kotlinw.hibernate.core.api.runJpaTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.hibernate.SessionFactory
import org.hibernate.engine.spi.SessionFactoryImplementor
import org.hibernate.event.service.spi.EventListenerRegistry
import org.hibernate.event.spi.EventType
import org.hibernate.event.spi.PostCollectionRecreateEvent
import org.hibernate.event.spi.PostCollectionRecreateEventListener
import org.hibernate.event.spi.PostCollectionRemoveEvent
import org.hibernate.event.spi.PostCollectionRemoveEventListener
import org.hibernate.event.spi.PostCollectionUpdateEvent
import org.hibernate.event.spi.PostCollectionUpdateEventListener
import org.hibernate.event.spi.PostCommitDeleteEventListener
import org.hibernate.event.spi.PostCommitInsertEventListener
import org.hibernate.event.spi.PostCommitUpdateEventListener
import org.hibernate.event.spi.PostDeleteEvent
import org.hibernate.event.spi.PostInsertEvent
import org.hibernate.event.spi.PostUpdateEvent
import org.hibernate.persister.entity.EntityPersister
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.OnConstruction
import xyz.kotlinw.eventbus.inprocess.InProcessEventBus
import xyz.kotlinw.eventbus.inprocess.LocalEvent
import xyz.kotlinw.jpa.api.JpaSessionContext
import xyz.kotlinw.jpa.api.ReactiveJpaContext

data class EntityChangeEvent(
    val entityTypes: List<KClass<*>>
) : LocalEvent()

interface ReactiveQuerySupportService {

    fun <T> reactiveJpaTask(block: ReactiveJpaContext.() -> T): T // TODO itt is Flow-nak kellene szerepelni
}

@Component
class ReactiveQuerySupportServiceImpl(
    private val sessionFactory: SessionFactory,
    private val eventBus: InProcessEventBus<*>
) : ReactiveQuerySupportService {

    private val eventListener =
        object : PostCommitInsertEventListener, PostCommitUpdateEventListener, PostCommitDeleteEventListener,
            PostCollectionRecreateEventListener, PostCollectionUpdateEventListener, PostCollectionRemoveEventListener {

            override fun requiresPostCommitHandling(persister: EntityPersister): Boolean = true

            override fun onPostDelete(event: PostDeleteEvent) {
                println(">> onPostDelete: ${event.persister.entityName}") // TODO reactive
            }

            override fun onPostDeleteCommitFailed(event: PostDeleteEvent) {
            }

            override fun onPostInsert(event: PostInsertEvent) {
                println(">> onPostInsert: ${event.persister.entityName}") // TODO reactive
            }

            override fun onPostInsertCommitFailed(event: PostInsertEvent) {
            }

            override fun onPostUpdate(event: PostUpdateEvent) {
                println(">> onPostUpdate: ${event.persister.entityName}") // TODO reactive
            }

            override fun onPostUpdateCommitFailed(event: PostUpdateEvent) {
            }

            override fun onPostRecreateCollection(event: PostCollectionRecreateEvent) {
                println(">> onPostRecreateCollection: ${event.affectedOwnerEntityName}") // TODO reactive
            }

            override fun onPostUpdateCollection(event: PostCollectionUpdateEvent) {
                println(">> onPostUpdateCollection: ${event.affectedOwnerEntityName}") // TODO reactive
            }

            override fun onPostRemoveCollection(event: PostCollectionRemoveEvent) {
                println(">> onPostRemoveCollection: ${event.affectedOwnerEntityName}") // TODO reactive
            }
        }

    private inner class ReactiveJpaContextImpl : ReactiveJpaContext {

        // TODO debounce?
        override fun <T> watchEntities(watchedEntityTypes: List<KClass<*>>, block: JpaSessionContext.() -> T): Flow<T> =
            flow {
                emit(sessionFactory.runJpaTask(block))

                eventBus.events()
                    .filterIsInstance<EntityChangeEvent>()
                    .filter { it.entityTypes.any { watchedEntityTypes.contains(it) } }
                    .collect {
                        emit(
                            sessionFactory.runJpaTask(block)
                        )
                    }
            }
    }

    @OnConstruction
    fun initialize() {
        with(
            sessionFactory.unwrap(SessionFactoryImplementor::class.java).serviceRegistry.getService(
                EventListenerRegistry::class.java
            )!!
        ) {
            appendListeners(EventType.POST_COMMIT_INSERT, eventListener)
            appendListeners(EventType.POST_COMMIT_UPDATE, eventListener)
            appendListeners(EventType.POST_COMMIT_DELETE, eventListener)
            appendListeners(EventType.POST_COLLECTION_RECREATE, eventListener)
            appendListeners(EventType.POST_COLLECTION_UPDATE, eventListener)
            appendListeners(EventType.POST_COLLECTION_REMOVE, eventListener)
        }
    }

    override fun <T> reactiveJpaTask(block: ReactiveJpaContext.() -> T): T = block(ReactiveJpaContextImpl())
}
