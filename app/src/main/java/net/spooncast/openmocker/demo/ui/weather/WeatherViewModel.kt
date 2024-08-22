package net.spooncast.openmocker.demo.ui.weather

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.spooncast.openmocker.demo.usecase.GetWeather
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val getWeather: GetWeather
): ViewModel() {

    var uiState by mutableStateOf<WeatherUiState>(WeatherUiState.Loading)
        private set

    init {
        onClickGetWeather()
    }

    fun onClickGetWeather() {
        uiState = WeatherUiState.Loading

        viewModelScope.launch {
            getWeather.get()
                .onSuccess {
                    Log.d("demo", "${TAG} Success : ${it}")
                    uiState = WeatherUiState.Success(it)
                }
                .onFailure {
                    Log.e("demo", "${TAG} Failure : ${it}")
                    uiState = WeatherUiState.Error(it)
                }
        }
    }

    companion object {
        private const val TAG = "WeatherViewModel"
    }
}