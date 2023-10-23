package xyz.kotlinw.io

class ResourceResolutionException(val resource: Resource, message: String? = null, cause: Throwable? = null) :
    RuntimeException(message, cause)
