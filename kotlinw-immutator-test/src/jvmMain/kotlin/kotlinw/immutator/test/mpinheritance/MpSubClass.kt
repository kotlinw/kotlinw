package kotlinw.immutator.test.mpinheritance

import kotlinw.immutator.annotation.Immutate
import kotlinw.immutator.test.mp.MpSuperClass

@Immutate
interface MpSubClass: MpSuperClass {
}
