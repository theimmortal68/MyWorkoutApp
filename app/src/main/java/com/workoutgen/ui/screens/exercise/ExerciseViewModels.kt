package com.workoutgen.ui.screens.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutgen.domain.model.*
import com.workoutgen.domain.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ============================================================================
// Exercise Library ViewModel
// ============================================================================

data class ExerciseLibraryUiState(
    val allExercises: List<Exercise> = emptyList(),
    val filteredExercises: List<Exercise> = emptyList(),
    val isLoading: Boolean = true,
    
    // Search
    val searchQuery: String = "",
    
    // Quick filters (single selection)
    val selectedMuscle: MuscleGroup? = null,
    val selectedDifficulty: Difficulty? = null,
    
    // Advanced filters (multi-selection)
    val selectedMuscles: Set<MuscleGroup> = emptySet(),
    val selectedEquipment: Set<Equipment> = emptySet(),
    val selectedDifficulties: Set<Difficulty> = emptySet(),
    val selectedTypes: Set<ExerciseType> = emptySet(),
    val bodyweightOnly: Boolean = false
) {
    val activeFilterCount: Int
        get() = listOf(
            selectedMuscles.isNotEmpty(),
            selectedEquipment.isNotEmpty(),
            selectedDifficulties.isNotEmpty(),
            selectedTypes.isNotEmpty(),
            bodyweightOnly,
            selectedMuscle != null,
            selectedDifficulty != null
        ).count { it }
}

@HiltViewModel
class ExerciseLibraryViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ExerciseLibraryUiState())
    val uiState: StateFlow<ExerciseLibraryUiState> = _uiState.asStateFlow()
    
    init {
        viewModelScope.launch {
            exerciseRepository.getAllExercises().collect { exercises ->
                _uiState.update { state ->
                    state.copy(
                        allExercises = exercises,
                        filteredExercises = applyFilters(exercises, state),
                        isLoading = false
                    )
                }
            }
        }
    }
    
    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            val newState = state.copy(searchQuery = query)
            newState.copy(filteredExercises = applyFilters(state.allExercises, newState))
        }
    }
    
    // Quick filters (single selection)
    fun setMuscleFilter(muscle: MuscleGroup?) {
        _uiState.update { state ->
            val newState = state.copy(selectedMuscle = muscle)
            newState.copy(filteredExercises = applyFilters(state.allExercises, newState))
        }
    }
    
    fun setDifficultyFilter(difficulty: Difficulty?) {
        _uiState.update { state ->
            val newState = state.copy(selectedDifficulty = difficulty)
            newState.copy(filteredExercises = applyFilters(state.allExercises, newState))
        }
    }
    
    fun setEquipmentFilter(equipment: Equipment?) {
        _uiState.update { state ->
            val newState = state.copy(
                selectedEquipment = if (equipment != null) setOf(equipment) else emptySet()
            )
            newState.copy(filteredExercises = applyFilters(state.allExercises, newState))
        }
    }
    
    // Advanced filters (multi-selection toggles)
    fun toggleMuscleFilter(muscle: MuscleGroup) {
        _uiState.update { state ->
            val newMuscles = if (muscle in state.selectedMuscles) {
                state.selectedMuscles - muscle
            } else {
                state.selectedMuscles + muscle
            }
            val newState = state.copy(selectedMuscles = newMuscles, selectedMuscle = null)
            newState.copy(filteredExercises = applyFilters(state.allExercises, newState))
        }
    }
    
    fun toggleEquipmentFilter(equipment: Equipment) {
        _uiState.update { state ->
            val newEquipment = if (equipment in state.selectedEquipment) {
                state.selectedEquipment - equipment
            } else {
                state.selectedEquipment + equipment
            }
            val newState = state.copy(selectedEquipment = newEquipment)
            newState.copy(filteredExercises = applyFilters(state.allExercises, newState))
        }
    }
    
    fun toggleDifficultyFilter(difficulty: Difficulty) {
        _uiState.update { state ->
            val newDifficulties = if (difficulty in state.selectedDifficulties) {
                state.selectedDifficulties - difficulty
            } else {
                state.selectedDifficulties + difficulty
            }
            val newState = state.copy(selectedDifficulties = newDifficulties, selectedDifficulty = null)
            newState.copy(filteredExercises = applyFilters(state.allExercises, newState))
        }
    }
    
    fun toggleTypeFilter(type: ExerciseType) {
        _uiState.update { state ->
            val newTypes = if (type in state.selectedTypes) {
                state.selectedTypes - type
            } else {
                state.selectedTypes + type
            }
            val newState = state.copy(selectedTypes = newTypes)
            newState.copy(filteredExercises = applyFilters(state.allExercises, newState))
        }
    }
    
    fun toggleBodyweightOnly() {
        _uiState.update { state ->
            val newState = state.copy(bodyweightOnly = !state.bodyweightOnly)
            newState.copy(filteredExercises = applyFilters(state.allExercises, newState))
        }
    }
    
    fun clearAllFilters() {
        _uiState.update { state ->
            val newState = state.copy(
                searchQuery = "",
                selectedMuscle = null,
                selectedDifficulty = null,
                selectedMuscles = emptySet(),
                selectedEquipment = emptySet(),
                selectedDifficulties = emptySet(),
                selectedTypes = emptySet(),
                bodyweightOnly = false
            )
            newState.copy(filteredExercises = state.allExercises)
        }
    }
    
    private fun applyFilters(exercises: List<Exercise>, state: ExerciseLibraryUiState): List<Exercise> {
        return exercises.filter { exercise ->
            // Search query
            val matchesSearch = state.searchQuery.isEmpty() ||
                exercise.name.contains(state.searchQuery, ignoreCase = true) ||
                exercise.description.contains(state.searchQuery, ignoreCase = true) ||
                exercise.primaryMuscles.any { it.displayName.contains(state.searchQuery, ignoreCase = true) }
            
            // Quick muscle filter
            val matchesQuickMuscle = state.selectedMuscle == null ||
                state.selectedMuscle in exercise.primaryMuscles
            
            // Quick difficulty filter
            val matchesQuickDifficulty = state.selectedDifficulty == null ||
                exercise.difficulty == state.selectedDifficulty
            
            // Advanced muscle filter
            val matchesMuscles = state.selectedMuscles.isEmpty() ||
                exercise.primaryMuscles.any { it in state.selectedMuscles } ||
                exercise.secondaryMuscles.any { it in state.selectedMuscles }
            
            // Equipment filter
            val matchesEquipment = state.selectedEquipment.isEmpty() ||
                exercise.equipmentRequired.any { it in state.selectedEquipment }
            
            // Advanced difficulty filter
            val matchesDifficulties = state.selectedDifficulties.isEmpty() ||
                exercise.difficulty in state.selectedDifficulties
            
            // Type filter
            val matchesTypes = state.selectedTypes.isEmpty() ||
                exercise.type in state.selectedTypes
            
            // Bodyweight only
            val matchesBodyweight = !state.bodyweightOnly ||
                exercise.equipmentRequired.isEmpty() ||
                exercise.equipmentRequired == listOf(Equipment.NONE)
            
            matchesSearch && matchesQuickMuscle && matchesQuickDifficulty &&
                matchesMuscles && matchesEquipment && matchesDifficulties &&
                matchesTypes && matchesBodyweight
        }
    }
}

// ============================================================================
// Exercise Detail ViewModel
// ============================================================================

data class ExerciseDetailUiState(
    val exercise: Exercise? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ExerciseDetailUiState())
    val uiState: StateFlow<ExerciseDetailUiState> = _uiState.asStateFlow()
    
    fun loadExercise(exerciseId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val exercise = exerciseRepository.getExerciseById(exerciseId)
            _uiState.update { it.copy(exercise = exercise, isLoading = false) }
        }
    }
}
