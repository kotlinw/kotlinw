package kotlinw.immutator.test.mpinheritance

import kotlinw.immutator.annotation.Immutate
import kotlinw.immutator.annotation.MpSuperClass2
import kotlinw.immutator.test.mp.MpSuperClass
import kotlin.test.Test
import kotlin.test.assertTrue

class MpInheritanceTest {
    @Test
    fun test() {
        val o = MpSubClassImmutable("a")
    }

    // TODO test for error message when a super-interface is from another module
}
