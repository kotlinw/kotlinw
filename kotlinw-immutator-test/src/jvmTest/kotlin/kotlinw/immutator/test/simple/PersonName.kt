package kotlinw.immutator.test.simple

import kotlinw.immutator.api.Immutate

@Immutate
sealed interface PersonName {
    val title: String?

    val firstName: String

    val lastName: String

    val fullName get() = (if (title != null) "$title " else "") + firstName + " " + lastName
}
