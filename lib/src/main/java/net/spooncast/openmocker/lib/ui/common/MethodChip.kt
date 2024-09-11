package net.spooncast.openmocker.lib.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
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

@Composable
internal fun MethodChip(
    method: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = method,
        modifier = modifier
            .widthIn(min = 55.dp)
            .background(Color.Black, CircleShape)
            .clip(CircleShape)
            .padding(2.dp),
        color = Color.White,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.titleSmall
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