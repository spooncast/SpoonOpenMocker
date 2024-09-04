package net.spooncast.openmocker.lib.ui.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.spooncast.openmocker.lib.ui.common.PreviewWithCondition

@Composable
fun CodeItem(
    code: Int,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onSelect: () -> Unit
) {
    val backgroundColor = if (selected) Color.Black else Color.White
    val borderColor = if (selected) Color.Transparent else Color.Black
    val textColor = if (selected) Color.White else Color.Black

    Text(
        text = "${code}",
        modifier = modifier
            .width(50.dp)
            .background(backgroundColor, CircleShape)
            .border(1.dp, borderColor, CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onSelect)
            .padding(5.dp),
        color = textColor,
        textAlign = TextAlign.Center
    )
}

@PreviewWithCondition
@Composable
private fun PreviewCodeItem() {
    MaterialTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            CodeItem(
                code = 200,
                onSelect = {}
            )
            CodeItem(
                code = 200,
                selected = true,
                onSelect = {}
            )
        }
    }
}