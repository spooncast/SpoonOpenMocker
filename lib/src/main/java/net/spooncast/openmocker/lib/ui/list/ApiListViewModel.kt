package net.spooncast.openmocker.lib.ui.list

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import net.spooncast.openmocker.lib.model.CachedKey
import net.spooncast.openmocker.lib.model.CachedValue
import net.spooncast.openmocker.lib.data.repo.CacheRepo
import net.spooncast.openmocker.lib.ui.list.dialog.ApiListDialogState

internal class ApiListViewModel(
    private val cacheRepo: CacheRepo
): ViewModel() {

    val items = cacheRepo.cachedMap

    var showDialog: ApiListDialogState by mutableStateOf(ApiListDialogState.None)
        private set


    fun onClickClearAll() {
        cacheRepo.clearCache()
    }

    fun onLongClick(key: CachedKey, value: CachedValue) {
        if (value.mock == null) return
        showDialog = ApiListDialogState.UnMock(key)
    }

    fun unMock(key: CachedKey) {
        showDialog = ApiListDialogState.None
        cacheRepo.unMock(key)
    }

    fun hideDialog() {
        showDialog = ApiListDialogState.None
    }

    companion object {
        fun provideFactory(repo: CacheRepo): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ApiListViewModel(repo)
            }
        }
    }
}