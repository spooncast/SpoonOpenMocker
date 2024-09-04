package net.spooncast.openmocker.lib.ui.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import net.spooncast.openmocker.lib.model.CachedKey
import net.spooncast.openmocker.lib.model.CachedResponse
import net.spooncast.openmocker.lib.repo.CacheRepo
import net.spooncast.openmocker.lib.ui.Destination

class ApiDetailViewModel(
    private val detail: Destination.Detail,
    private val repo: CacheRepo
): ViewModel() {

    var apiDetail by mutableStateOf(detail)
        private set

    var close = MutableSharedFlow<Unit>()
        private set

    fun onClickComplete(
        method: String,
        path: String,
        code: Int,
        body: String
    ) {
        val key = CachedKey(method, path)
        val response = CachedResponse(code, body)
        repo.mock(key, response)
        viewModelScope.launch { close.emit(Unit) }
    }

    companion object {
        fun provideFactory(
            detail: Destination.Detail,
            repo: CacheRepo
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ApiDetailViewModel(detail, repo)
            }
        }
    }
}