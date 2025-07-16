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
import androidx.compose.runtime.mutableLongStateOf
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
import net.spooncast.openmocker.lib.ui.common.ApiItem
import net.spooncast.openmocker.lib.ui.common.PreviewWithCondition
import net.spooncast.openmocker.lib.ui.common.TopBar
import net.spooncast.openmocker.lib.ui.common.VerticalSpacer

private val successCodes = listOf(200, 201, 202)
private val failureCodes = listOf(
    400 to R.string.common_response_code_400,
    401 to R.string.common_response_code_401,
    403 to R.string.common_response_code_403,
    404 to R.string.common_response_code_404,
    500 to R.string.common_response_code_500
)
private val durations = listOf(0L, 1_000L, 3_000L, 5_000L, 10_000L)

@Composable
internal fun ApiDetailPane(
    vm: ApiDetailViewModel,
    onBackPressed: () -> Unit
) {
    var updatedCode by remember { mutableIntStateOf(vm.code) }
    var updatedBody by remember { mutableStateOf(vm.body) }
    var updatedDuration by remember { mutableLongStateOf(vm.duration) }

    LaunchedEffect(key1 = Unit) {
        vm.close.collect { onBackPressed() }
    }

    Scaffold(
        topBar = {
            DetailTopBar(
                onBackPressed = onBackPressed,
                onClickSave = { vm.onClickSave(updatedCode, updatedBody, updatedDuration) }
            )
        }
    ) {
        Pane(
            method = vm.method,
            path = vm.path,
            code = updatedCode,
            body = updatedBody,
            duration = updatedDuration,
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(15.dp),
            onUpdateCode = { code -> updatedCode = code },
            onUpdateBody = { body -> updatedBody = body },
            onUpdateDuration = { duration -> updatedDuration = duration }
        )
    }
}

@Composable
private fun Pane(
    method: String,
    path: String,
    code: Int,
    body: String,
    duration: Long,
    modifier: Modifier = Modifier,
    onUpdateCode: (Int) -> Unit,
    onUpdateBody: (String) -> Unit,
    onUpdateDuration: (Long) -> Unit,
) {
    Column(
        modifier = modifier
    ) {
        ApiItem(
            method = method,
            path = path
        )
        VerticalSpacer(size = 15.dp)
        UpdateResponseCodeArea(
            updatedCode = code,
            onUpdateCode = onUpdateCode
        )
        VerticalSpacer(size = 15.dp)
        UpdateDurationArea(
            duration = duration,
            onUpdate = onUpdateDuration
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
                onDismissRequest = { expanded = false },
                modifier = Modifier.padding(horizontal = 5.dp)
            ) {
                successCodes.forEach { code ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "${code} (${stringResource(id = R.string.common_success)})",
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        onClick = {
                            onUpdateCode(code)
                            expanded = false
                        }
                    )
                }
                failureCodes.forEach { (code, stringResId) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "${code} (${stringResource(id = stringResId)})",
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        onClick = {
                            onUpdateCode(code)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdateDurationArea(
    duration: Long,
    modifier: Modifier = Modifier,
    onUpdate: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.update_duration),
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
                    text = "${duration}",
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
                durations.forEach { duration ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "${duration} ms",
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        onClick = {
                            onUpdate(duration)
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

        TextField(
            value = updatedBody,
            onValueChange = onUpdateBody,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        )
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
            code = 200,
            body = body,
            duration = 0L,
            modifier = Modifier
                .fillMaxSize()
                .padding(15.dp),
            onUpdateCode = {},
            onUpdateBody = {},
            onUpdateDuration = {}
        )
    }
}