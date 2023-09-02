package com.example.lantian_front.framework

import androidx.lifecycle.ViewModel

abstract class BaseViewModel<
        ACTION : BaseViewModel.Action,
        IMMUTABLE_UI_STATE,
        MUTABLE_UI_STATE : IMMUTABLE_UI_STATE,
        > :
    ViewModel() {

    protected var innerUISate: MUTABLE_UI_STATE = this.getInitialUIState()
    val uiState: IMMUTABLE_UI_STATE = innerUISate

    abstract fun reduce(action: ACTION)
    abstract fun getInitialUIState(): MUTABLE_UI_STATE

    abstract class Action

    abstract class Event

}