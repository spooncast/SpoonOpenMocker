package net.spooncast.openmocker.lib.ui

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable
import net.spooncast.openmocker.lib.repo.MemCacheRepoImpl
import net.spooncast.openmocker.lib.ui.list.ApiListPane
import net.spooncast.openmocker.lib.ui.list.ApiListViewModel

sealed interface Destination {
    @Serializable
    object List

    @Serializable
    object Detail
}

@Composable
fun OpenMockerApp() {
    val context = LocalContext.current
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Destination.List
    ) {
        composable<Destination.List> {
            val cacheRepo = MemCacheRepoImpl.getInstance()
            val viewModel: ApiListViewModel = viewModel(
                factory = ApiListViewModel.provideFactory(cacheRepo)
            )
            ApiListPane(
                vm = viewModel,
                onBackPressed = { (context as Activity).finish() }
            )
        }
        composable<Destination.Detail> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "detail screen")
            }
        }
    }
}