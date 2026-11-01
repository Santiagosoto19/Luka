package co.luka.capture

import kotlinx.coroutines.flow.MutableStateFlow

object ListenerHealth {
    val connected = MutableStateFlow(false)
}
