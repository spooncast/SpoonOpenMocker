package net.spooncast.openmocker.lib.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import net.spooncast.openmocker.lib.model.CachedKey
import net.spooncast.openmocker.lib.ui.common.HorizontalSpacer
import net.spooncast.openmocker.lib.ui.common.PreviewWithCondition

@Composable
fun SelectCodeDialog(
    key: CachedKey,
    code: Int,
    onDismiss: () -> Unit,
    onClick: (CachedKey, Int) -> Unit
) {
    var updatedCode by remember { mutableStateOf("${code}") }

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(text = "Modify http status code")

            TextField(
                value = updatedCode,
                onValueChange = {
                    updatedCode = it
                },
                isError = updatedCode.toIntOrNull() == null,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
            )

            Row(
                modifier = Modifier.align(Alignment.End)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.width(100.dp)
                ) {
                    Text(text = "Cancel")
                }
                HorizontalSpacer(size = 10.dp)
                Button(
                    onClick = {
                        onClick(key, updatedCode.toIntOrNull() ?: code)
                    },
                    modifier = Modifier.width(100.dp),
                    enabled = updatedCode.toIntOrNull() != null
                ) {
                    Text(text = "Ok")
                }
            }
        }
    }
}

@PreviewWithCondition
@Composable
private fun PreviewSelectCodeDialog() {
    MaterialTheme {
        SelectCodeDialog(
            key = CachedKey(
                method = "GET",
                path = "/mock/1234"
            ),
            code = 200,
            onDismiss = {},
            onClick = { _, _ -> }
        )
    }
}