package net.spooncast.apimocker.lib.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import net.spooncast.apimocker.lib.model.MockerKey
import net.spooncast.apimocker.lib.model.MockerValue
import net.spooncast.apimocker.lib.ui.component.MockerItem

@Composable
fun MockerPane(
     onBackPressed: () -> Unit
) {
    Scaffold(
        topBar = {
            TopBar(onBackPressed = onBackPressed)
        }
    ) {
        // TODO : ViewModel 개발 후, 반영 예정 (items, onClick)
        Pane(
            items = emptyList(),
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            onClick = { _, _ -> }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit
) {
    TopAppBar(
        title = {
            Text(text = "Mocker")
        },
        modifier = modifier,
        navigationIcon = {
            Icon(
                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                contentDescription = "back",
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onBackPressed)
                    .padding(10.dp)
            )
        }
    )
}

@Composable
private fun Pane(
    items: List<Pair<MockerKey, MockerValue>>,
    modifier: Modifier = Modifier,
    onClick: (MockerKey, MockerValue) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        itemsIndexed(items) { index, (key, value) ->
            MockerItem(
                index = index + 1,
                key = key,
                value = value,
                onClick = { onClick(key, value) }
            )
        }
    }
}