package com.example.lantian_front.viewmodel

import android.app.Activity
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.viewModelScope
import com.example.lantian_front.framework.BaseViewModel
import com.example.lantian_front.model.TextRes
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class BusViewModel : BaseViewModel<BusViewModel.Action, Unit, Unit, BaseViewModel.AEvent>() {

    val snackbarHostState: SnackbarHostState = SnackbarHostState()
    sealed class Action : BaseViewModel.AAction() {
        class NeedActivity(val block: (activity: Activity) -> Unit) : Action()
        class ToastAction(val text: TextRes): Action()
        class TransferEvent(val event: BaseViewModel.AEvent): Action()
    }

    sealed class Event: BaseViewModel.AEvent {
        class NeedActivity(val block: (activity: Activity) -> Unit) : Event()
        class ToastEvent(val text: TextRes, val block: () -> Unit): Event()
    }

    override fun reduce(action: Action) {
        when (action) {
            is Action.NeedActivity -> {
                viewModelScope.launch {
                    innerEvent.emit(Event.NeedActivity(action.block))
                }
            }
            is Action.ToastAction -> {
                viewModelScope.launch {
                    innerEvent.emit(Event.ToastEvent(action.text) { })
                }
            }

            is Action.TransferEvent -> {
                viewModelScope.launch {
                    innerEvent.emit(action.event)
                }
            }
        }
    }

    override fun getInitialUIState() {
    }

}