package net.spooncast.mirage.demo.ui.weather

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import net.spooncast.mirage.demo.model.RespWeather
import net.spooncast.mirage.lib.ui.common.VerticalSpacer

@Composable
fun WeatherDetail(
    weather: RespWeather,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(weather.weather) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .border(1.dp, Color.Black)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(text = it.main)
                VerticalSpacer(size = 5.dp)
                Text(text = it.description)
            }
        }
    }
}