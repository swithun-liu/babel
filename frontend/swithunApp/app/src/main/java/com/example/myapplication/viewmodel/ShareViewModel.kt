package com.example.myapplication.viewmodel

import android.app.Activity
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.framework.BaseViewModel2
import com.example.myapplication.model.TextRes
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class ShareViewModel : BaseViewModel2<ShareViewModel.Action, Unit, Unit>() {
    private val innerUIEvent = MutableSharedFlow<Event>()
    val uiEvent = innerUIEvent.asSharedFlow()

    val snackbarHostState: SnackbarHostState = SnackbarHostState()
    sealed class Action : BaseViewModel2.Action() {
        class NeedActivity(val block: (activity: Activity) -> Unit) : Action()
        class ToastAction(val text: TextRes): Action()
    }

    sealed class Event: BaseViewModel2.Event() {
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

    suspend fun suspendReduce(action: Action) = suspendCancellableCoroutine { continuation ->
        when (action) {
            is Action.NeedActivity -> {
                viewModelScope.launch {
                    innerUIEvent.emit(Event.NeedActivity {
                        action.block.invoke(it)
                        continuation.resume(Unit)
                    })
                }
            }
            is Action.ToastAction -> {
                viewModelScope.launch {
                    innerUIEvent.emit(Event.ToastEvent(action.text) {
                        continuation.resume(Unit)
                    })
                }
            }
        }
    }

    override fun getInitialUIState() {
    }

}