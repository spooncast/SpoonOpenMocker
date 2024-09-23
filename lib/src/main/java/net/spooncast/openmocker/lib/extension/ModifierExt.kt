package net.spooncast.openmocker.lib.extension

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

@Composable
fun Modifier.singleClickable(
    enabled: Boolean = true,
    durationMillis: Long = 1_000L,
    onClick: () -> Unit,
): Modifier {
    val onClickMediator = remember(key1 = onClick) { MutableSharedFlow<() -> Unit>() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(key1 = Unit) {
        onClickMediator
            .throttleFirst(durationMillis)
            .collect { it.invoke() }
    }

    return this then Modifier.clickable(
        enabled = enabled,
        onClick = {
            coroutineScope.launch {
                onClickMediator.emit(onClick)
            }
        }
    )
}