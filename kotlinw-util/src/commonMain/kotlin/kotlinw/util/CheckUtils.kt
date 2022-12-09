package kotlinw.util

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun <T : Any> isNotNull(value: T?): Boolean {
    contract {
        returns(true) implies (value != null)
        returns(false) implies (value == null)
    }
    return value != null
}
