package net.spooncast.openmocker.lib.ui

import android.app.Activity
import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import net.spooncast.openmocker.lib.repo.MemCacheRepoImpl
import net.spooncast.openmocker.lib.ui.detail.ApiDetailPane
import net.spooncast.openmocker.lib.ui.detail.ApiDetailViewModel
import net.spooncast.openmocker.lib.ui.list.ApiListPane
import net.spooncast.openmocker.lib.ui.list.ApiListViewModel

sealed interface Destination {
    @Serializable
    object List

    @Serializable
    @Parcelize
    data class Detail(
        val method: String,
        val path: String,
        val code: Int,
        val body: String
    ): Parcelable
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
                onBackPressed = { (context as Activity).finish() },
                onClickDetail = { key, value ->
                    val code = value.mock?.code ?: value.response.code
                    val body = value.mock?.body ?: value.response.body
                    navController.navigate(Destination.Detail(key.method, key.path, code, body))
                }
            )
        }
        composable<Destination.Detail> {
            val cacheRepo = MemCacheRepoImpl.getInstance()
            val viewModel: ApiDetailViewModel = viewModel(
                factory = ApiDetailViewModel.provideFactory(cacheRepo)
            )
            ApiDetailPane(
                vm = viewModel,
                onBackPressed = navController::popBackStack
            )
        }
    }
}