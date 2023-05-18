package kotlinw.immutator.test.mpinheritance

import kotlinw.immutator.annotation.Immutate

@Immutate
sealed interface MpSuperClass {

    companion object

    val a: String
}
