package net.spooncast.openmocker.lib.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.spooncast.openmocker.lib.R
import net.spooncast.openmocker.lib.model.CachedKey
import net.spooncast.openmocker.lib.model.CachedValue
import net.spooncast.openmocker.lib.ui.common.TopBar
import net.spooncast.openmocker.lib.ui.list.component.ApiItemWithStatus
import net.spooncast.openmocker.lib.ui.list.dialog.ApiListDialogState
import net.spooncast.openmocker.lib.ui.list.dialog.UnMockDialog

@Composable
fun ApiListPane(
    vm: ApiListViewModel,
    onBackPressed: () -> Unit,
    onClickDetail: (CachedKey, CachedValue) -> Unit
) {
    when (val state = vm.showDialog) {
        is ApiListDialogState.UnMock -> {
            UnMockDialog(
                onDismiss = vm::hideDialog,
                onClickOk = { vm.unMock(state.key) }
            )
        }
        else -> { /* Do nothing */ }
    }
    
    Scaffold(
        topBar = {
            TopBar(
                title = "API List",
                onBackPressed = onBackPressed,
                actions = {
                    Text(
                        text = stringResource(id = R.string.common_clear),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = vm::onClickClearAll)
                            .padding(10.dp)
                    )
                }
            )
        }
    ) {
        Pane(
            items = vm.items.toList(),
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            onClick = onClickDetail,
            onLongClick = vm::onLongClick
        )
    }
}

@Composable
private fun Pane(
    items: List<Pair<CachedKey, CachedValue>>,
    modifier: Modifier = Modifier,
    onClick: (CachedKey, CachedValue) -> Unit,
    onLongClick: (CachedKey, CachedValue) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        items(items) { (key, value) ->
            ApiItemWithStatus(
                key = key,
                value = value,
                onClick = { onClick(key, value) },
                onLongClick = { onLongClick(key, value) }
            )
        }
    }
}