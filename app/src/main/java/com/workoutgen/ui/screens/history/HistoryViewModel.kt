package com.workoutgen.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutgen.domain.model.WorkoutLog
import com.workoutgen.domain.repository.WorkoutLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val logs: List<WorkoutLog> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val workoutLogRepository: WorkoutLogRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    
    init {
        viewModelScope.launch {
            workoutLogRepository.getAllLogs().collect { logs ->
                _uiState.update { it.copy(logs = logs, isLoading = false) }
            }
        }
    }
}
