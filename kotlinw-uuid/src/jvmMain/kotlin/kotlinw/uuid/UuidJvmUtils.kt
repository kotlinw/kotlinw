package kotlinw.uuid

import java.util.UUID

fun Uuid.toJavaUuid(): UUID = value

fun UUID.toUuid(): Uuid = Uuid(this)
