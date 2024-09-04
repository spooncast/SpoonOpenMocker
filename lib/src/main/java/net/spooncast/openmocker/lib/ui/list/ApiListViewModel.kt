package net.spooncast.openmocker.lib.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import net.spooncast.openmocker.lib.repo.CacheRepo

class ApiListViewModel(
    private val cacheRepo: CacheRepo
): ViewModel() {

    val items = cacheRepo.cachedMap

    fun onClickClearAll() {
        cacheRepo.clearCache()
    }

    companion object {
        fun provideFactory(repo: CacheRepo): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ApiListViewModel(repo)
            }
        }
    }
}