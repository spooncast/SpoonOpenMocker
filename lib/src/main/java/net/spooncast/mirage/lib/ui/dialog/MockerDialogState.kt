package net.spooncast.mirage.lib.ui.dialog

import net.spooncast.mirage.lib.model.MockerKey

sealed interface MockerDialogState {

    data object None: MockerDialogState

    data class SelectCode(
        val key: MockerKey,
        val code: Int
    ): MockerDialogState
}