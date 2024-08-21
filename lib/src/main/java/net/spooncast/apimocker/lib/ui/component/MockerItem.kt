package net.spooncast.apimocker.lib.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
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
import net.spooncast.apimocker.lib.model.MockerKey
import net.spooncast.apimocker.lib.model.MockerResponse
import net.spooncast.apimocker.lib.model.MockerValue
import net.spooncast.apimocker.lib.ui.common.PreviewWithCondition
import net.spooncast.apimocker.lib.ui.common.VerticalSpacer

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MockerItem(
    index: Int,
    key: MockerKey,
    value: MockerValue,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isMocking = value.mocked != null
    val code = value.mocked?.code ?: value.response.code

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
private fun PreviewMockerItem() {
    val response = MockerResponse(200, "")
    val mocked = MockerResponse(401, "")

    MaterialTheme {
        Column {
            MockerItem(
                index = 1,
                key = MockerKey(
                    method = "GET",
                    path = "/mock/1234",
                ),
                value = MockerValue(
                    response = response
                ),
                onClick = {}
            )
            MockerItem(
                index = 1,
                key = MockerKey(
                    method = "GET",
                    path = "/mock/1234",
                ),
                value = MockerValue(
                    response = response,
                    mocked = mocked
                ),
                onClick = {}
            )
        }
    }
}
