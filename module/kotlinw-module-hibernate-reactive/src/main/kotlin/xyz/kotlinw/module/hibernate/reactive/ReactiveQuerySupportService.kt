package xyz.kotlinw.module.hibernate.reactive

import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf
import kotlinw.hibernate.core.service.GlobalTransactionListener
import kotlinw.hibernate.core.service.JpaPersistenceService
import kotlinw.util.stdlib.collection.ConcurrentHashMap
import kotlinw.util.stdlib.collection.ConcurrentHashSet
import kotlinw.util.stdlib.collection.ConcurrentMutableMap
import kotlinw.util.stdlib.collection.ConcurrentMutableSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import org.hibernate.Hibernate
import org.hibernate.engine.spi.SessionFactoryImplementor
import org.hibernate.engine.spi.SessionImplementor
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.event.service.spi.EventListenerRegistry
import org.hibernate.event.spi.EventSource
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
import xyz.kotlinw.eventbus.inprocess.launchPublish
import xyz.kotlinw.jpa.api.JpaSessionContext
import xyz.kotlinw.jpa.api.ReactiveJpaContext
import xyz.kotlinw.jpa.repository.AbstractEntity
import xyz.kotlinw.module.core.ApplicationCoroutineService

data class EntityChangeEvent(
    val changedEntityTypes: Collection<KClass<*>>
) : LocalEvent()

interface ReactiveQuerySupportService {

    fun <T : Flow<*>> reactiveJpaTask(block: ReactiveJpaContext.() -> T): T
}

@Component
class ReactiveQuerySupportServiceImpl(
    private val jpaPersistenceService: JpaPersistenceService,
    private val eventBus: InProcessEventBus<Any>,
    private val applicationCoroutineService: ApplicationCoroutineService
) : ReactiveQuerySupportService {

    private val entityChangeEventListener =
        object : PostCommitInsertEventListener, PostCommitUpdateEventListener, PostCommitDeleteEventListener,
            PostCollectionRecreateEventListener, PostCollectionUpdateEventListener, PostCollectionRemoveEventListener {

            override fun requiresPostCommitHandling(persister: EntityPersister): Boolean = true

            override fun onPostDelete(event: PostDeleteEvent) {
                registerChangedEntity(event.session, event.entity)
            }

            override fun onPostDeleteCommitFailed(event: PostDeleteEvent) {
            }

            override fun onPostInsert(event: PostInsertEvent) {
                registerChangedEntity(event.session, event.entity)
            }

            override fun onPostInsertCommitFailed(event: PostInsertEvent) {
            }

            override fun onPostUpdate(event: PostUpdateEvent) {
                registerChangedEntity(event.session, event.entity)
            }

            override fun onPostUpdateCommitFailed(event: PostUpdateEvent) {
            }

            // TODO valahol olvastam, hogy ez mindig meghívódik a collection inicializálásakor, szóval ezt másképp kellene kezelni
            override fun onPostRecreateCollection(event: PostCollectionRecreateEvent) {
                registerChangedEntity(event.session, event.affectedOwnerOrNull)
            }

            override fun onPostUpdateCollection(event: PostCollectionUpdateEvent) {
                registerChangedEntity(event.session, event.affectedOwnerOrNull)
            }

            override fun onPostRemoveCollection(event: PostCollectionRemoveEvent) {
                registerChangedEntity(event.session, event.affectedOwnerOrNull)
            }
        }

    private data class ActiveTransactionEntityChangeData(
        val changedEntityTypes: ConcurrentMutableSet<KClass<*>> = ConcurrentHashSet()
    )

    private val activeTransactionMap: ConcurrentMutableMap<SharedSessionContractImplementor, ActiveTransactionEntityChangeData> =
        ConcurrentHashMap()

    private inner class ReactiveJpaContextImpl : ReactiveJpaContext {

        // TODO debounce?
        override fun <T> watchEntities(watchedEntityTypes: List<KClass<*>>, block: JpaSessionContext.() -> T): Flow<T> {
            // merge() is used to prevent missing entity change events because of a slow collector of the initial execution.
            return merge(
                // Initial execution
                flow {
                    emit(jpaPersistenceService.runJpaTask(block))
                },
                // Executions caused by entity change events
                flow {
                    eventBus.events()
                        .filterIsInstance<EntityChangeEvent>()
                        .filter {
                            it.changedEntityTypes.any { changedEntityClass ->
                                watchedEntityTypes.any { watchedEntityClass ->
                                    watchedEntityClass.isSuperclassOf(
                                        changedEntityClass
                                    )
                                }
                            }
                        }
                        .collect {
                            emit(jpaPersistenceService.runJpaTask(block))
                        }
                }
            )
        }
    }

    @OnConstruction
    fun initialize() {
        with(
            jpaPersistenceService.sessionFactory.unwrap(SessionFactoryImplementor::class.java).serviceRegistry.getService(
                EventListenerRegistry::class.java
            )!!
        ) {
            appendListeners(EventType.POST_COMMIT_INSERT, entityChangeEventListener)
            appendListeners(EventType.POST_COMMIT_UPDATE, entityChangeEventListener)
            appendListeners(EventType.POST_COMMIT_DELETE, entityChangeEventListener)
            appendListeners(EventType.POST_COLLECTION_RECREATE, entityChangeEventListener)
            appendListeners(EventType.POST_COLLECTION_UPDATE, entityChangeEventListener)
            appendListeners(EventType.POST_COLLECTION_REMOVE, entityChangeEventListener)
        }

        jpaPersistenceService.addGlobalTransactionListener(object : GlobalTransactionListener {

            override fun afterBegin(session: SharedSessionContractImplementor) {
                if (session.supportsEvents()) {
                    activeTransactionMap[session] = ActiveTransactionEntityChangeData()
                }
            }

            override fun beforeCompletion(session: SharedSessionContractImplementor) {
            }

            override fun afterCompletion(
                session: SharedSessionContractImplementor,
                successful: Boolean,
                delayed: Boolean
            ) {
                if (session.supportsEvents() && successful) {
                    with(applicationCoroutineService.applicationCoroutineScope) {
                        println(">> reactive: fire ${activeTransactionMap.getValue(session).changedEntityTypes}")
                        eventBus.launchPublish(
                            EntityChangeEvent(activeTransactionMap.getValue(session).changedEntityTypes)
                        )
                    }
                }
            }

            override fun afterDispose(session: SharedSessionContractImplementor) {
                if (session.supportsEvents()) {
                    activeTransactionMap.remove(session)
                }
            }
        })
    }

    private fun SharedSessionContractImplementor.supportsEvents() = this is SessionImplementor

    private fun registerChangedEntity(eventSource: EventSource, entity: Any?) {
        if (entity is AbstractEntity<*>) {
            val entityKClass = Hibernate.getClassLazy(entity).kotlin
            activeTransactionMap.getValue(eventSource.session).changedEntityTypes.add(entityKClass)
        }
    }

    override fun <T : Flow<*>> reactiveJpaTask(block: ReactiveJpaContext.() -> T): T = block(ReactiveJpaContextImpl())
}
