package net.spooncast.openmocker.lib.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import net.spooncast.openmocker.lib.model.CachedKey
import net.spooncast.openmocker.lib.model.CachedValue
import net.spooncast.openmocker.lib.ui.common.TopBar
import net.spooncast.openmocker.lib.ui.list.component.ApiItem

@Composable
fun ApiListPane(
    vm: ApiListViewModel,
    onBackPressed: () -> Unit,
    onClickDetail: (CachedKey, CachedValue) -> Unit
) {
    Scaffold(
        topBar = {
            TopBar(
                title = "API List",
                onBackPressed = onBackPressed,
                actions = {
                    Text(
                        text = "Clear All",
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
            onClick = onClickDetail
        )
    }
}

@Composable
private fun Pane(
    items: List<Pair<CachedKey, CachedValue>>,
    modifier: Modifier = Modifier,
    onClick: (CachedKey, CachedValue) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        itemsIndexed(items) { index, (key, value) ->
            ApiItem(
                index = index + 1,
                key = key,
                value = value,
                onClick = { onClick(key, value) }
            )
        }
    }
}