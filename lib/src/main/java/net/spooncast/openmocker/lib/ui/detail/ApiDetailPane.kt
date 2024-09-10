package net.spooncast.openmocker.lib.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.spooncast.openmocker.lib.R
import net.spooncast.openmocker.lib.ui.common.PreviewWithCondition
import net.spooncast.openmocker.lib.ui.common.TopBar
import net.spooncast.openmocker.lib.ui.common.TwoButtons
import net.spooncast.openmocker.lib.ui.common.VerticalSpacer

private val successCodes = listOf(200, 201, 202)
private val failureCodes = listOf(400, 401, 403, 404, 500)

@Composable
fun ApiDetailPane(
    vm: ApiDetailViewModel,
    onBackPressed: () -> Unit
) {
    LaunchedEffect(key1 = Unit) {
        vm.close.collect { onBackPressed() }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(id = R.string.title_api_detail),
                onBackPressed = onBackPressed
            )
        }
    ) {
        Pane(
            method = vm.method,
            path = vm.path,
            code = vm.code,
            body = vm.body,
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            onClickClose = onBackPressed,
            onClickModify = vm::onClickComplete
        )
    }
}

@Composable
private fun Pane(
    method: String,
    path: String,
    code: Int,
    body: String,
    modifier: Modifier = Modifier,
    onClickClose: () -> Unit,
    onClickModify: (String, String, Int, String) -> Unit,
) {
    var selectedCode by remember { mutableIntStateOf(code) }
    var updatedBody by remember { mutableStateOf(body) }

    Column(
        modifier = modifier.padding(15.dp)
    ) {
        UpdateResponseCodeArea(
            selectedCode = selectedCode,
            onSelectCode = { selectedCode = it}
        )

        VerticalSpacer(size = 25.dp)

        UpdateResponseBodyArea(
            selectedCode = selectedCode,
            updatedBody = updatedBody,
            modifier = Modifier.weight(1F, true),
            onUpdateBody = { updatedBody = it }
        )

        VerticalSpacer(size = 25.dp)

        TwoButtons(
            onClickCancel = onClickClose,
            onClickOk = {
                onClickModify(method, path, selectedCode, updatedBody)
            }
        )
    }
}

@Composable
private fun UpdateResponseCodeArea(
    selectedCode: Int,
    modifier: Modifier = Modifier,
    onSelectCode: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.update_response_code),
            modifier = Modifier.weight(1F, true),
            style = MaterialTheme.typography.titleMedium
        )
        Box(
            modifier = Modifier
                .weight(1F, true)
                .clickable { expanded = true },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${selectedCode}",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.titleMedium,
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    contentDescription = null
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                (successCodes + failureCodes).forEach { code ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "${code}",
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        onClick = {
                            onSelectCode(code)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdateResponseBodyArea(
    selectedCode: Int,
    updatedBody: String,
    modifier: Modifier = Modifier,
    onUpdateBody: (String) -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(id = R.string.update_response_body),
            style = MaterialTheme.typography.titleLarge
        )
        VerticalSpacer(size = 15.dp)

        if (selectedCode in successCodes) {
            TextField(
                value = updatedBody,
                onValueChange = onUpdateBody,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1F, true),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.update_restrictions),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@PreviewWithCondition
@Composable
private fun PreviewPane() {
    val body = """
        {
            "a": 111,
            "b": 222,
            "c": 333
        }
    """.trimIndent()
    MaterialTheme {
        Pane(
            method = "GET",
            path = "/api/test",
            code = 401,
            body = body,
            modifier = Modifier.fillMaxSize(),
            onClickClose = {},
            onClickModify = { _, _, _, _ -> }
        )
    }
}