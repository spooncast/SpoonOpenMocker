package net.spooncast.openmocker.lib.ui.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
fun MethodChip(
    method: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = method,
        modifier = modifier
            .widthIn(min = 60.dp)
            .background(Color.Black, CircleShape)
            .clip(CircleShape)
            .padding(5.dp),
        color = Color.White,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.titleMedium
    )
}

@PreviewWithCondition
@Composable
private fun PreviewMethodChip() {
    MaterialTheme {
        MethodChip(
            method = "GET",
        )
    }
}