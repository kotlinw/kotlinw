package kotlinw.remoting.api.client

import kotlinw.remoting.api.SupportsRemoting

/**
 * This interface should not be implemented directly, only by annotating an interface with [SupportsRemoting].
 */
interface ClientProxy<T : Any>
