package net.spooncast.openmocker.lib.ui.dialog

import net.spooncast.openmocker.lib.model.OpenMockerKey

sealed interface OpenMockerDialogState {

    data object None: OpenMockerDialogState

    data class SelectCode(
        val key: OpenMockerKey,
        val code: Int
    ): OpenMockerDialogState
}