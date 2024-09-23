package net.spooncast.openmocker.lib.ui.list.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.spooncast.openmocker.lib.model.CachedKey
import net.spooncast.openmocker.lib.model.CachedResponse
import net.spooncast.openmocker.lib.model.CachedValue
import net.spooncast.openmocker.lib.ui.common.ApiItem
import net.spooncast.openmocker.lib.ui.common.PreviewWithCondition
import net.spooncast.openmocker.lib.ui.common.VerticalSpacer

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ApiItemWithStatus(
    key: CachedKey,
    value: CachedValue,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val codeColor = if (value.isCodeMocked) Color.Red else Color.Black
    val durationColor = if (value.isDurationMocked) Color.Red else Color.Black
    val bodyColor = if (value.isBodyMocked) Color.Red else Color.Black

    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onLongClick = onLongClick,
                onDoubleClick = { /* Do not handle */ },
                onClick = onClick
            )
            .padding(horizontal = 15.dp, vertical = 10.dp),
    ) {
        ApiItem(
            method = key.method,
            path = key.path
        )
        VerticalSpacer(size = 10.dp)
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "${value.code}",
                modifier = Modifier.weight(1F, true),
                color = codeColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = "${value.duration} ms",
                modifier = Modifier.weight(1F, true),
                color = durationColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = "body",
                modifier = Modifier.weight(1F, true),
                color = bodyColor,
                textAlign = TextAlign.Center
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = 5.dp, bottom = 5.dp),
            thickness = (0.1).dp,
            color = Color.Black
        )
    }
}

@PreviewWithCondition
@Composable
private fun PreviewApiItemLegacy() {
    val response = CachedResponse(200, "")
    val mocked = CachedResponse(401, "")

    MaterialTheme {
        Column {
            ApiItemWithStatus(
                key = CachedKey(
                    method = "GET",
                    path = "/mock/1234asdfasfasdfasdfasdfasdfasdfasfasdfasdfasdfasdfasd",
                ),
                value = CachedValue(
                    response = response
                ),
                onClick = {},
                onLongClick = {}
            )
            ApiItemWithStatus(
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
