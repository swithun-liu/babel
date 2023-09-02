package com.example.lantian_front.viewmodel

import android.app.Activity
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.viewModelScope
import com.example.lantian_front.framework.BaseViewModel
import com.example.lantian_front.model.TextRes
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class ShareViewModel : BaseViewModel<ShareViewModel.Action, Unit, Unit>() {
    private val innerUIEvent = MutableSharedFlow<Event>()
    val uiEvent = innerUIEvent.asSharedFlow()

    val snackbarHostState: SnackbarHostState = SnackbarHostState()
    sealed class Action : BaseViewModel.Action() {
        class NeedActivity(val block: (activity: Activity) -> Unit) : Action()
        class ToastAction(val text: TextRes): Action()
    }

    sealed class Event: BaseViewModel.Event() {
        class NeedActivity(val block: (activity: Activity) -> Unit) : Event()
        class ToastEvent(val text: TextRes, val block: () -> Unit): Event()
    }

    override fun reduce(action: Action) {
        when (action) {
            is Action.NeedActivity -> {
                viewModelScope.launch {
                    innerUIEvent.emit(Event.NeedActivity(action.block))
                }
            }
            is Action.ToastAction -> {
                viewModelScope.launch {
                    innerUIEvent.emit(Event.ToastEvent(action.text) { })
                }
            }
        }
    }

    override fun getInitialUIState() {
    }

}