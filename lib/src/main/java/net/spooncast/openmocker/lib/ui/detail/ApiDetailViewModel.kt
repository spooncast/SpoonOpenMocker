package net.spooncast.openmocker.lib.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import net.spooncast.openmocker.lib.model.CachedKey
import net.spooncast.openmocker.lib.model.CachedResponse
import net.spooncast.openmocker.lib.repo.CacheRepo

internal class ApiDetailViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repo: CacheRepo
): ViewModel() {

    val method = checkNotNull(savedStateHandle.get<String>("method"))
    val path = checkNotNull(savedStateHandle.get<String>("path"))
    val code = checkNotNull(savedStateHandle.get<Int>("code"))
    val body = checkNotNull(savedStateHandle.get<String>("body"))
    val duration = checkNotNull(savedStateHandle.get<Long>("duration"))

    var close = MutableSharedFlow<Unit>()
        private set

    fun onClickSave(
        code: Int,
        body: String,
        duration: Long
    ) {
        val key = CachedKey(method, path)
        val response = CachedResponse(code, body, duration)
        repo.mock(key, response)
        viewModelScope.launch { close.emit(Unit) }
    }

    companion object {
        fun provideFactory(
            repo: CacheRepo
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ApiDetailViewModel(createSavedStateHandle(), repo)
            }
        }
    }
}