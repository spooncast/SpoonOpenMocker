package net.spooncast.openmocker.lib.ui.list.dialog

import net.spooncast.openmocker.lib.model.CachedKey

sealed interface ApiListDialogState {

    data object None: ApiListDialogState

    data class UnMock(val key: CachedKey): ApiListDialogState
}