package net.spooncast.openmocker.lib.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.spooncast.openmocker.lib.R
import net.spooncast.openmocker.lib.ui.common.HorizontalSpacer
import net.spooncast.openmocker.lib.ui.common.PreviewWithCondition
import net.spooncast.openmocker.lib.ui.common.TopBar
import net.spooncast.openmocker.lib.ui.common.VerticalSpacer
import net.spooncast.openmocker.lib.ui.detail.component.MethodChip

private val successCodes = listOf(200, 201, 202)
private val failureCodes = listOf(400, 401, 403, 404, 500)

@Composable
fun ApiDetailPane(
    vm: ApiDetailViewModel,
    onBackPressed: () -> Unit
) {
    var updatedCode by remember { mutableIntStateOf(vm.code) }
    var updatedBody by remember { mutableStateOf(vm.body) }

    LaunchedEffect(key1 = Unit) {
        vm.close.collect { onBackPressed() }
    }

    Scaffold(
        topBar = {
            DetailTopBar(
                onBackPressed = onBackPressed,
                onClickSave = { vm.onClickSave(updatedCode, updatedBody) }
            )
        }
    ) {
        Pane(
            method = vm.method,
            path = vm.path,
            code = updatedCode,
            body = updatedBody,
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(15.dp),
            onUpdateCode = { code -> updatedCode = code },
            onUpdateBody = { body -> updatedBody = body }
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
    onUpdateCode: (Int) -> Unit,
    onUpdateBody: (String) -> Unit
) {
    Column(
        modifier = modifier
    ) {
        DetailHeader(
            method = method,
            path = path
        )
        VerticalSpacer(size = 15.dp)
        UpdateResponseCodeArea(
            updatedCode = code,
            onUpdateCode = onUpdateCode
        )
        VerticalSpacer(size = 15.dp)
        UpdateResponseBodyArea(
            updatedCode = code,
            updatedBody = body,
            modifier = Modifier.weight(1F, true),
            onUpdateBody = onUpdateBody
        )
    }
}

@Composable
private fun DetailTopBar(
    onBackPressed: () -> Unit,
    onClickSave: () -> Unit
) {
    TopBar(
        title = stringResource(id = R.string.title_api_detail),
        onBackPressed = onBackPressed,
        actions = {
            Text(
                text = stringResource(id = R.string.common_save),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onClickSave)
                    .padding(10.dp)
            )
        }
    )
}

@Composable
private fun DetailHeader(
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

@Composable
private fun UpdateResponseCodeArea(
    updatedCode: Int,
    modifier: Modifier = Modifier,
    onUpdateCode: (Int) -> Unit
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
                .height(30.dp)
                .clickable { expanded = true },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${updatedCode}",
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
                            onUpdateCode(code)
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
    updatedCode: Int,
    updatedBody: String,
    modifier: Modifier = Modifier,
    onUpdateBody: (String) -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(id = R.string.update_response_body),
            style = MaterialTheme.typography.titleMedium
        )
        VerticalSpacer(size = 15.dp)

        if (updatedCode in successCodes) {
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
            path = "/weather?lat=44.34&lon=10.99&appId=12341234123412341234",
            code = 401,
            body = body,
            modifier = Modifier
                .fillMaxSize()
                .padding(15.dp),
            onUpdateCode = {},
            onUpdateBody = {},
        )
    }
}