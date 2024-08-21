package net.spooncast.apimocker.lib.ui.dialog

import net.spooncast.apimocker.lib.model.MockerKey
import net.spooncast.apimocker.lib.model.MockerValue

sealed interface MockerDialogState {

    data object None: MockerDialogState

    data class SelectCode(
        val key: MockerKey,
        val value: MockerValue
    ): MockerDialogState
}