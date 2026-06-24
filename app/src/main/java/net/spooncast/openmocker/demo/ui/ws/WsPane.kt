package net.spooncast.openmocker.demo.ui.ws

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun WsPane(
    vm: WsViewModel = hiltViewModel()
) {
    val listState = rememberLazyListState()

    // 새 메시지가 도착하면 최신 항목으로 스크롤한다.
    LaunchedEffect(vm.messages.size) {
        if (vm.messages.isNotEmpty()) {
            listState.animateScrollToItem(vm.messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Header(
            connected = vm.connected,
            count = vm.messages.size,
            onClear = vm::clear,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1F),
            contentAlignment = Alignment.Center,
        ) {
            if (vm.messages.isEmpty()) {
                Text(
                    text = "수신 메시지가 없습니다.\nIDE 플러그인 WsPanel 에서 Inject 하면 여기에 표시됩니다.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(vm.messages, key = { it.seq }) { message ->
                        MessageRow(message)
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(
    connected: Boolean,
    count: Int,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ConnectionChip(connected)
        Text(
            text = "  수신 $count 건",
            style = MaterialTheme.typography.bodyMedium,
        )
        Box(modifier = Modifier.weight(1F))
        OutlinedButton(onClick = onClear) {
            Text(text = "Clear")
        }
    }
}

@Composable
private fun ConnectionChip(connected: Boolean) {
    val label = if (connected) "● Connected" else "○ Disconnected"
    val color = if (connected) Color(0xFF2E7D32) else Color(0xFF9E9E9E)
    Text(
        text = label,
        color = color,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12F))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun MessageRow(message: WsMessage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = "#${message.seq}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = message.text,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
