package net.spooncast.openmocker.demo.ui.ws

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.spooncast.openmocker.demo.repo.ChatSocketClient
import javax.inject.Inject

/**
 * [ChatSocketClient.incoming] 을 구독해 수신 메시지를 화면 상태로 누적한다.
 *
 * 데모에서는 OpenMocker injector([net.spooncast.openmocker.demo.DemoEventInjector])가 주입한 payload 가
 * 이 스트림으로 도착한다 — 즉 플러그인 Inject 가 곧 여기 표시되는 메시지가 된다.
 */
@HiltViewModel
class WsViewModel @Inject constructor(
    private val client: ChatSocketClient,
) : ViewModel() {

    val messages = mutableStateListOf<WsMessage>()

    var connected by mutableStateOf(false)
        private set

    init {
        client.connect()

        viewModelScope.launch {
            client.connected.collect { connected = it }
        }
        viewModelScope.launch {
            client.incoming.collect { payload ->
                messages.add(WsMessage(seq = messages.size + 1, text = payload))
            }
        }
    }

    fun clear() {
        messages.clear()
    }
}
