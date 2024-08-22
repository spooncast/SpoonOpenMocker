package net.spooncast.openmocker.demo.ui.weather

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import net.spooncast.openmocker.lib.OpenMockerActivity
import net.spooncast.openmocker.lib.ui.common.VerticalSpacer

@Composable
fun WeatherPane(
    vm: WeatherViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopBar(
                onClickOpenMocker = {
                    val intent = Intent(context, OpenMockerActivity::class.java)
                    context.startActivity(intent)
                }
            )
        }
    ) {
        Pane(
            state = vm.uiState,
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            onClickGetWeather = vm::onClickGetWeather
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    modifier: Modifier = Modifier,
    onClickOpenMocker: () -> Unit
) {
    TopAppBar(
        title = {
            Text(text = "Weather")
        },
        modifier = modifier,
        actions = {
            Text(
                text = "Mocker",
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onClickOpenMocker)
                    .padding(10.dp)
            )
        }
    )
}

@Composable
private fun Pane(
    state: WeatherUiState,
    modifier: Modifier = Modifier,
    onClickGetWeather: () -> Unit
) {
    Column(
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1F, true),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                is WeatherUiState.Success -> {
                    WeatherDetail(state.weather)
                }
                is WeatherUiState.Loading -> {
                    CircularProgressIndicator()
                }
                is WeatherUiState.Error -> {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(text = "에러 발생")
                        VerticalSpacer(size = 10.dp)
                        Text(text = state.throwable.message ?: "-")
                    }
                }
            }
        }
        Button(
            onClick = onClickGetWeather,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(text = "날씨 가져오기")
        }
    }
}