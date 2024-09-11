package net.spooncast.openmocker.lib.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.spooncast.openmocker.lib.R

@Composable
internal fun TwoButtons(
    modifier: Modifier = Modifier,
    onClickCancel: () -> Unit,
    onClickOk: () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onClickCancel,
            modifier = Modifier.weight(1F, true)
        ) {
            Text(text = stringResource(id = R.string.common_cancel))
        }
        Button(
            onClick = onClickOk,
            modifier = Modifier.weight(1F, true)
        ) {
            Text(text = stringResource(id = R.string.common_ok))
        }
    }
}