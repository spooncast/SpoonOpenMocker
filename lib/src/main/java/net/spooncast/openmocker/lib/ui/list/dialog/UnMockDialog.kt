package net.spooncast.openmocker.lib.ui.list.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import net.spooncast.openmocker.lib.R
import net.spooncast.openmocker.lib.ui.common.PreviewWithCondition
import net.spooncast.openmocker.lib.ui.common.TwoButtons
import net.spooncast.openmocker.lib.ui.common.VerticalSpacer

@Composable
internal fun UnMockDialog(
    onDismiss: () -> Unit,
    onClickOk: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(id = R.string.open_mocker),
                style = MaterialTheme.typography.titleMedium
            )
            VerticalSpacer(size = 10.dp)

            Text(text = stringResource(id = R.string.release_mocking))

            VerticalSpacer(size = 20.dp)

            TwoButtons(
                onClickCancel = onDismiss,
                onClickOk = onClickOk
            )
        }
    }
}

@PreviewWithCondition
@Composable
private fun PreviewUnMockDialog() {
    MaterialTheme {
        UnMockDialog(
            onDismiss = {},
            onClickOk = {}
        )
    }
}