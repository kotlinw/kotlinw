package kotlinw.uuid

import java.util.UUID

fun Uuid.asJavaUuid(): UUID = value

fun UUID.toUuid(): Uuid = Uuid(this)
