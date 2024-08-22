package net.spooncast.openmocker.lib

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import dagger.hilt.android.AndroidEntryPoint
import net.spooncast.openmocker.lib.ui.OpenMockerPane

@AndroidEntryPoint
class OpenMockerActivity: ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                OpenMockerPane(
                    onBackPressed = ::finish
                )
            }
        }
    }
}