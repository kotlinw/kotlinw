package kotlinw.immutator.test.mpinheritance

import kotlinw.immutator.annotation.Immutate

@Immutate
sealed interface MpSubClass: MpSuperClass {

    companion object

}
