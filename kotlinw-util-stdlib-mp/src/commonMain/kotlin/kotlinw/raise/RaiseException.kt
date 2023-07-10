package kotlinw.raise

abstract class RaiseException(val raiseCause: Any): RuntimeException(raiseCause.toString())
