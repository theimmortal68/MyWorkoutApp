package com.workoutgen.ui.screens.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutgen.domain.model.Workout
import com.workoutgen.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkoutDetailUiState(
    val workout: Workout? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class WorkoutDetailViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(WorkoutDetailUiState())
    val uiState: StateFlow<WorkoutDetailUiState> = _uiState.asStateFlow()
    
    fun loadWorkout(workoutId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val workout = workoutRepository.getWorkoutById(workoutId)
            _uiState.update { state ->
                state.copy(
                    workout = workout,
                    isLoading = false,
                    error = if (workout == null) "Workout not found" else null
                )
            }
        }
    }
    
    fun toggleFavorite() {
        val workoutId = _uiState.value.workout?.id ?: return
        viewModelScope.launch {
            workoutRepository.toggleFavorite(workoutId)
            // Reload to get updated state
            loadWorkout(workoutId)
        }
    }
}
