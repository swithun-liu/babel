package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel

abstract class BaseViewModel<ACTION: BaseViewModel.Action>: ViewModel() {

    abstract fun reduce(action: ACTION)

    abstract class Action

}