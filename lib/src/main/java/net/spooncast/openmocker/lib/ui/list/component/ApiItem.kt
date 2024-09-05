package net.spooncast.openmocker.lib.ui.list.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import net.spooncast.openmocker.lib.model.CachedKey
import net.spooncast.openmocker.lib.model.CachedResponse
import net.spooncast.openmocker.lib.model.CachedValue
import net.spooncast.openmocker.lib.ui.common.PreviewWithCondition
import net.spooncast.openmocker.lib.ui.common.VerticalSpacer

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ApiItem(
    index: Int,
    key: CachedKey,
    value: CachedValue,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isMocking = value.mock != null
    val code = value.mock?.code ?: value.response.code

    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onLongClick = onLongClick,
                onClick = onClick
            )
            .padding(horizontal = 15.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "${index}")
        Column(
            modifier = Modifier.weight(1F, true)
        ) {
            Text(text = key.method)
            VerticalSpacer(size = 5.dp)
            Text(
                text = key.path,
                modifier = Modifier.basicMarquee()
            )
        }
        Text(text = "${code}")
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "",
            tint = if (isMocking) Color.Green else Color.LightGray
        )
    }
}

@PreviewWithCondition
@Composable
private fun PreviewApiItem() {
    val response = CachedResponse(200, "")
    val mocked = CachedResponse(401, "")

    MaterialTheme {
        Column {
            ApiItem(
                index = 1,
                key = CachedKey(
                    method = "GET",
                    path = "/mock/1234",
                ),
                value = CachedValue(
                    response = response
                ),
                onClick = {},
                onLongClick = {}
            )
            ApiItem(
                index = 1,
                key = CachedKey(
                    method = "GET",
                    path = "/mock/1234",
                ),
                value = CachedValue(
                    response = response,
                    mock = mocked
                ),
                onClick = {},
                onLongClick = {}
            )
        }
    }
}
