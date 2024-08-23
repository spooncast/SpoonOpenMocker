package net.spooncast.openmocker.lib.ui.dialog

import net.spooncast.openmocker.lib.model.CachedKey

sealed interface OpenMockerDialogState {

    data object None: OpenMockerDialogState

    data class SelectCode(
        val key: CachedKey,
        val code: Int
    ): OpenMockerDialogState
}