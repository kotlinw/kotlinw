package kotlinw.util.stdlib

import java.net.URL

fun Url.toJavaURL() = URL(value)

fun URL.toKotlinwUrl() = Url(this.toExternalForm())
