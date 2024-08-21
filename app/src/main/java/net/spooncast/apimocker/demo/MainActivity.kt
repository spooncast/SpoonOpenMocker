package net.spooncast.apimocker.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import net.spooncast.apimocker.demo.ui.theme.SpoonAndroidApiMockerTheme
import net.spooncast.apimocker.demo.ui.weather.WeatherPane

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpoonAndroidApiMockerTheme {
                WeatherPane()
            }
        }
    }
}