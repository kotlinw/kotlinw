package xyz.kotlinw.module.core

import kotlinx.coroutines.CoroutineScope

interface ApplicationCoroutineService {

    val applicationCoroutineScope: CoroutineScope
}
