package net.spooncast.openmocker.lib.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import net.spooncast.openmocker.lib.model.OpenMockerKey
import net.spooncast.openmocker.lib.model.OpenMockerResponse
import net.spooncast.openmocker.lib.model.OpenMockerValue
import net.spooncast.openmocker.lib.repo.OpenMockerRepo
import net.spooncast.openmocker.lib.ui.dialog.OpenMockerDialogState
import javax.inject.Inject

@HiltViewModel
class OpenMockerViewModel @Inject constructor(
    private val openMockerRepo: OpenMockerRepo
): ViewModel() {

    val items = openMockerRepo.cachedMap

    var dialogState by mutableStateOf<OpenMockerDialogState>(OpenMockerDialogState.None)
        private set

    fun onClick(key: OpenMockerKey, value: OpenMockerValue) {
        if (value.mocked == null) {
            dialogState = OpenMockerDialogState.SelectCode(key, value.response.code)
            return
        }

        openMockerRepo.unMock(key)
    }

    fun onClickModifyCode(key: OpenMockerKey, code: Int) {
        dialogState = OpenMockerDialogState.None
        openMockerRepo.mock(key = key, response = OpenMockerResponse(code = code, body = ""))
    }

    fun onClickClearAll() {
        openMockerRepo.clearCache()
    }

    fun hideDialog() {
        dialogState = OpenMockerDialogState.None
    }
}