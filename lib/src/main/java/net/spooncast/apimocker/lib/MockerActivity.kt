package net.spooncast.apimocker.lib

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import dagger.hilt.android.AndroidEntryPoint
import net.spooncast.apimocker.lib.ui.MockerPane

@AndroidEntryPoint
class MockerActivity: ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MockerPane(
                    onBackPressed = ::finish
                )
            }
        }
    }
}