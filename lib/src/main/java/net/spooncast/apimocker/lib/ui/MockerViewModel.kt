package net.spooncast.apimocker.lib.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import net.spooncast.apimocker.lib.model.MockerKey
import net.spooncast.apimocker.lib.model.MockerResponse
import net.spooncast.apimocker.lib.model.MockerValue
import net.spooncast.apimocker.lib.repo.MockerRepo
import net.spooncast.apimocker.lib.ui.dialog.MockerDialogState
import javax.inject.Inject

@HiltViewModel
class MockerViewModel @Inject constructor(
    private val mockerRepo: MockerRepo
): ViewModel() {

    val items = mockerRepo.cachedMap

    var dialogState by mutableStateOf<MockerDialogState>(MockerDialogState.None)
        private set

    fun onClick(key: MockerKey, value: MockerValue) {
        if (value.mocked == null) {
            dialogState = MockerDialogState.SelectCode(key, value.response.code)
            return
        }

        mockerRepo.unMock(key)
    }

    fun onClickModifyCode(key: MockerKey, code: Int) {
        dialogState = MockerDialogState.None
        mockerRepo.mock(key = key, response = MockerResponse(code = code, body = ""))
    }

    fun onClickClearAll() {
        mockerRepo.clearCache()
    }

    fun hideDialog() {
        dialogState = MockerDialogState.None
    }
}