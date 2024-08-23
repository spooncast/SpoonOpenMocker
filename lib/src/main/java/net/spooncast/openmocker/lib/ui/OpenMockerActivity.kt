package net.spooncast.openmocker.lib.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import net.spooncast.openmocker.lib.repo.MemCacheRepoImpl

class OpenMockerActivity: ComponentActivity() {

    private val viewModel: OpenMockerViewModel by viewModels {
        val cacheRepo = MemCacheRepoImpl.getInstance()
        OpenMockerViewModel.provideFactory(cacheRepo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                OpenMockerPane(
                    vm = viewModel,
                    onBackPressed = ::finish
                )
            }
        }
    }
}