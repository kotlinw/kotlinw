package kotlinw.module.hibernate.core

import arrow.core.continuations.AtomicRef
import kotlinx.atomicfu.atomic
import org.hibernate.boot.registry.BootstrapServiceRegistry
import org.hibernate.service.Service
import org.hibernate.service.ServiceRegistry
import org.hibernate.service.spi.ServiceBinding
import org.hibernate.service.spi.ServiceRegistryImplementor

internal class BootstrapServiceRegistryProxy : BootstrapServiceRegistry, ServiceRegistryImplementor {

    private val delegateHolder = atomic<BootstrapServiceRegistry?>(null)

    private val delegate
        get() = delegateHolder.value ?: throw IllegalStateException("Delegate of $this is not initialized yet.")

    fun initialize(delegate: BootstrapServiceRegistry) {
        check(delegateHolder.value == null)
        delegateHolder.value = delegate
    }

    override fun close() = delegate.close()

    override fun getParentServiceRegistry(): ServiceRegistry? = delegate.parentServiceRegistry

    override fun <R : Service> getService(serviceRole: Class<R>): R? = delegate.getService(serviceRole)

    override fun <R : Service> locateServiceBinding(serviceRole: Class<R>): ServiceBinding<R>? = (delegate as ServiceRegistryImplementor).locateServiceBinding(serviceRole)

    override fun destroy()  = (delegate as ServiceRegistryImplementor).destroy()

    override fun registerChild(child: ServiceRegistryImplementor)  =  (delegate as ServiceRegistryImplementor).registerChild(child)

    override fun deRegisterChild(child: ServiceRegistryImplementor)   = (delegate as ServiceRegistryImplementor).deRegisterChild(child)

    override fun <T : Service> fromRegistryOrChildren(serviceRole: Class<T>): T?   = (delegate as ServiceRegistryImplementor).fromRegistryOrChildren(serviceRole)
}
