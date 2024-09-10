package net.spooncast.openmocker.lib.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.spooncast.openmocker.lib.ui.detail.component.MethodChip

@Composable
fun ApiItem(
    method: String,
    path: String,
    modifier: Modifier = Modifier
) {
    var lineCnt by remember { mutableIntStateOf(1) }
    val alignment = if (lineCnt == 1) Alignment.CenterVertically else Alignment.Top

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = alignment
    ) {
        MethodChip(method = method)
        HorizontalSpacer(size = 15.dp)
        Text(
            text = path,
            onTextLayout = { result -> lineCnt = result.lineCount },
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@PreviewWithCondition
@Composable
private fun PreviewApiItem() {
    MaterialTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ApiItem(
                method = "GET",
                path = "/weather?lat=44.34&lon=10.99&appId=12341234123412341234"
            )
            ApiItem(
                method = "GET",
                path = "/weather"
            )
        }
    }
}