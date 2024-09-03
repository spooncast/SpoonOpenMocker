package net.spooncast.openmocker.lib.ui.list

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import net.spooncast.openmocker.lib.model.CachedKey
import net.spooncast.openmocker.lib.model.CachedResponse
import net.spooncast.openmocker.lib.model.CachedValue
import net.spooncast.openmocker.lib.repo.CacheRepo
import net.spooncast.openmocker.lib.ui.dialog.OpenMockerDialogState

class ListViewModel(
    private val cacheRepo: CacheRepo
): ViewModel() {

    val items = cacheRepo.cachedMap

    var dialogState by mutableStateOf<OpenMockerDialogState>(OpenMockerDialogState.None)
        private set

    fun onClick(key: CachedKey, value: CachedValue) {
        if (value.mock == null) {
            dialogState = OpenMockerDialogState.SelectCode(key, value.response.code)
            return
        }

        cacheRepo.unMock(key)
    }

    fun onClickModifyCode(key: CachedKey, code: Int) {
        dialogState = OpenMockerDialogState.None
        cacheRepo.mock(key = key, response = CachedResponse(code = code, body = ""))
    }

    fun onClickClearAll() {
        cacheRepo.clearCache()
    }

    fun hideDialog() {
        dialogState = OpenMockerDialogState.None
    }

    companion object {
        fun provideFactory(repo: CacheRepo): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ListViewModel(repo)
            }
        }
    }
}