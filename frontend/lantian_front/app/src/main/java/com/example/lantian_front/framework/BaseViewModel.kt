package com.example.lantian_front.framework

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

abstract class BaseViewModel<
        ACTION : BaseViewModel.AAction,
        IMMUTABLE_UI_STATE,
        MUTABLE_UI_STATE : IMMUTABLE_UI_STATE,
        EVENT: BaseViewModel.AEvent
        > :
    ViewModel() {

    protected var innerUISate: MUTABLE_UI_STATE = this.getInitialUIState()
    val uiState: IMMUTABLE_UI_STATE = innerUISate

    protected var innerEvent: MutableSharedFlow<EVENT> = MutableSharedFlow()
    val event: SharedFlow<EVENT> = innerEvent.asSharedFlow()

    abstract fun reduce(action: ACTION)
    abstract fun getInitialUIState(): MUTABLE_UI_STATE

    abstract class AAction

    interface AEvent

}