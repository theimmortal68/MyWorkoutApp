package com.workoutgen.ui.screens.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutgen.domain.model.*
import com.workoutgen.domain.repository.UserProfileRepository
import com.workoutgen.domain.repository.WorkoutRepository
import com.workoutgen.domain.usecase.GenerateWorkoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GenerateWorkoutUiState(
    val selectedGoal: FitnessGoal = FitnessGoal.GENERAL_FITNESS,
    val selectedMuscles: Set<MuscleGroup> = emptySet(),
    val durationMinutes: Int = 45,
    val includeWarmup: Boolean = true,
    val includeCooldown: Boolean = true,
    val prioritizeCompound: Boolean = true,
    val isGenerating: Boolean = false,
    val generatedWorkoutId: String? = null,
    val error: String? = null
)

@HiltViewModel
class GenerateWorkoutViewModel @Inject constructor(
    private val generateWorkoutUseCase: GenerateWorkoutUseCase,
    private val workoutRepository: WorkoutRepository,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(GenerateWorkoutUiState())
    val uiState: StateFlow<GenerateWorkoutUiState> = _uiState.asStateFlow()
    
    private var userProfile: UserProfile = UserProfile()
    
    init {
        viewModelScope.launch {
            userProfileRepository.getUserProfile().collect { profile ->
                userProfile = profile ?: UserProfile()
                // Pre-fill with user's preferred settings
                _uiState.update { state ->
                    state.copy(
                        selectedGoal = userProfile.fitnessGoals.firstOrNull() ?: FitnessGoal.GENERAL_FITNESS,
                        durationMinutes = userProfile.preferredWorkoutDuration
                    )
                }
            }
        }
    }
    
    fun setGoal(goal: FitnessGoal) {
        _uiState.update { it.copy(selectedGoal = goal, error = null) }
    }
    
    fun toggleMuscle(muscle: MuscleGroup) {
        _uiState.update { state ->
            val newMuscles = if (muscle in state.selectedMuscles) {
                state.selectedMuscles - muscle
            } else {
                state.selectedMuscles + muscle
            }
            state.copy(selectedMuscles = newMuscles, error = null)
        }
    }
    
    fun setDuration(minutes: Int) {
        _uiState.update { it.copy(durationMinutes = minutes, error = null) }
    }
    
    fun setIncludeWarmup(include: Boolean) {
        _uiState.update { it.copy(includeWarmup = include) }
    }
    
    fun setIncludeCooldown(include: Boolean) {
        _uiState.update { it.copy(includeCooldown = include) }
    }
    
    fun setPrioritizeCompound(prioritize: Boolean) {
        _uiState.update { it.copy(prioritizeCompound = prioritize) }
    }
    
    fun generateWorkout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, error = null) }
            
            val params = WorkoutGenerationParams(
                userProfile = userProfile.copy(
                    fitnessGoals = listOf(_uiState.value.selectedGoal)
                ),
                targetMuscles = _uiState.value.selectedMuscles.toList().ifEmpty { null },
                durationMinutes = _uiState.value.durationMinutes,
                includeWarmup = _uiState.value.includeWarmup,
                includeCooldown = _uiState.value.includeCooldown,
                prioritizeCompound = _uiState.value.prioritizeCompound
            )
            
            generateWorkoutUseCase(params)
                .onSuccess { workout ->
                    // Save the workout
                    workoutRepository.saveWorkout(workout)
                    _uiState.update { state ->
                        state.copy(
                            isGenerating = false,
                            generatedWorkoutId = workout.id
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update { state ->
                        state.copy(
                            isGenerating = false,
                            error = exception.message ?: "Failed to generate workout"
                        )
                    }
                }
        }
    }
}
