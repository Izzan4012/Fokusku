package com.example.jetpack.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jetpack.ui.screens.FocusScreen
import com.example.jetpack.ui.screens.HistoryScreen
import com.example.jetpack.ui.viewmodel.AppViewModelProvider
import com.example.jetpack.ui.viewmodel.FocusViewModel
import com.example.jetpack.ui.viewmodel.HistoryViewModel

private data class Tab(val label: String, val icon: String)

private val tabs = listOf(Tab("Fokus", "⏱"), Tab("Riwayat", "📋"))

@Composable
fun FokusKuApp(
    focusViewModel: FocusViewModel = viewModel(factory = AppViewModelProvider.Factory),
    historyViewModel: HistoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    val focusState by focusViewModel.uiState.collectAsStateWithLifecycle()
    val historyState by historyViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        focusViewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Text(tab.icon) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (selectedTab) {
            0 -> FocusScreen(
                state = focusState,
                onSubjectChange = focusViewModel::onSubjectChange,
                onFlipModeChange = focusViewModel::onFlipModeChange,
                onStart = focusViewModel::start,
                onTogglePause = focusViewModel::togglePause,
                onFinish = focusViewModel::finishAndSave,
                onReset = focusViewModel::reset,
                modifier = contentModifier
            )
            else -> HistoryScreen(
                state = historyState,
                onUpdate = { historyViewModel.update(it) },
                onDelete = { historyViewModel.delete(it) },
                onDeleteAll = { historyViewModel.deleteAll() },
                modifier = contentModifier
            )
        }
    }
}
