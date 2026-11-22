package com.example.jetpack.ui.viewmodel

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.jetpack.FokusKuApplication

/** Factory untuk membuat ViewModel beserta dependensinya (repository). */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            val app = fokusKuApplication()
            FocusViewModel(app.sessionRepository, app.sensorRepository)
        }
        initializer {
            HistoryViewModel(fokusKuApplication().sessionRepository)
        }
    }
}

private fun CreationExtras.fokusKuApplication(): FokusKuApplication =
    this[APPLICATION_KEY] as FokusKuApplication
