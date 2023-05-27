package com.example.myapplication.framework

import androidx.lifecycle.ViewModel

abstract class BaseViewModel<ACTION: BaseViewModel.Action>: ViewModel() {

    abstract fun reduce(action: ACTION)

    abstract class Action

}