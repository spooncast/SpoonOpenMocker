package net.spooncast.openmocker.lib.ui

import android.app.Activity
import android.os.Parcelable
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
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

internal sealed interface Destination {
    @Serializable
    object List

    @Serializable
    @Parcelize
    data class Detail(
        val method: String,
        val path: String,
        val code: Int,
        val body: String,
        val duration: Long
    ): Parcelable
}

@Composable
internal fun OpenMockerApp() {
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
                    val duration = value.mock?.duration ?: value.response.duration
                    navController.navigate(Destination.Detail(key.method, key.path, code, body, duration))
                }
            )
        }
        composable<Destination.Detail>(
            enterTransition = {
                return@composable slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    tween(700)
                )
            },
            exitTransition = {
                return@composable slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    tween(700)
                )
            }
        ) {
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