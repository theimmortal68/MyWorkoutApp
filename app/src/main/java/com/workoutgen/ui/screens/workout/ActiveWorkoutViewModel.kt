package com.workoutgen.ui.screens.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutgen.domain.model.*
import com.workoutgen.domain.repository.WorkoutLogRepository
import com.workoutgen.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ActiveWorkoutUiState(
    val workout: Workout? = null,
    val currentExerciseIndex: Int = 0,
    val currentExercise: WorkoutExercise? = null,
    val completedSets: List<SetLog> = emptyList(),
    val upcomingExercises: List<WorkoutExercise> = emptyList(),
    val exerciseLogs: MutableMap<String, MutableList<SetLog>> = mutableMapOf(),
    val isResting: Boolean = false,
    val restTimeRemaining: Int = 0,
    val elapsedSeconds: Int = 0,
    val isLoading: Boolean = true,
    val isComplete: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val workoutLogRepository: WorkoutLogRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()
    
    private var timerJob: Job? = null
    private var restTimerJob: Job? = null
    private var startTime: Long = 0
    
    fun loadWorkout(workoutId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val workout = workoutRepository.getWorkoutById(workoutId)
            
            if (workout != null) {
                startTime = System.currentTimeMillis()
                startTimer()
                
                _uiState.update { state ->
                    state.copy(
                        workout = workout,
                        currentExercise = workout.exercises.firstOrNull(),
                        upcomingExercises = workout.exercises.drop(1),
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        error = "Workout not found"
                    )
                }
            }
        }
    }
    
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { state ->
                    state.copy(elapsedSeconds = state.elapsedSeconds + 1)
                }
            }
        }
    }
    
    fun logSet(reps: Int, weight: Float?) {
        val currentExercise = _uiState.value.currentExercise ?: return
        val setNumber = _uiState.value.completedSets.size + 1
        
        val setLog = SetLog(
            setNumber = setNumber,
            targetReps = currentExercise.reps.first,
            reps = reps,
            weight = weight,
            rpe = null
        )
        
        // Add to current sets
        val updatedSets = _uiState.value.completedSets + setLog
        
        // Store in exercise logs map
        val exerciseLogs = _uiState.value.exerciseLogs.toMutableMap()
        val exerciseSetList = exerciseLogs.getOrPut(currentExercise.exercise.id) { mutableListOf() }
        exerciseSetList.add(setLog)
        
        // Check if exercise is complete
        if (updatedSets.size >= currentExercise.sets) {
            // Move to next exercise
            moveToNextExercise(exerciseLogs)
        } else {
            // Start rest timer
            startRestTimer(currentExercise.restSeconds)
            _uiState.update { state ->
                state.copy(
                    completedSets = updatedSets,
                    exerciseLogs = exerciseLogs
                )
            }
        }
    }
    
    private fun startRestTimer(seconds: Int) {
        restTimerJob?.cancel()
        _uiState.update { it.copy(isResting = true, restTimeRemaining = seconds) }
        
        restTimerJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000)
                remaining--
                _uiState.update { it.copy(restTimeRemaining = remaining) }
            }
            _uiState.update { it.copy(isResting = false) }
        }
    }
    
    fun skipRest() {
        restTimerJob?.cancel()
        _uiState.update { it.copy(isResting = false, restTimeRemaining = 0) }
    }
    
    private fun moveToNextExercise(exerciseLogs: MutableMap<String, MutableList<SetLog>>) {
        val workout = _uiState.value.workout ?: return
        val nextIndex = _uiState.value.currentExerciseIndex + 1
        
        if (nextIndex >= workout.exercises.size) {
            // Workout complete
            _uiState.update { state ->
                state.copy(
                    currentExercise = null,
                    completedSets = emptyList(),
                    upcomingExercises = emptyList(),
                    exerciseLogs = exerciseLogs,
                    isResting = false
                )
            }
        } else {
            // Move to next exercise
            _uiState.update { state ->
                state.copy(
                    currentExerciseIndex = nextIndex,
                    currentExercise = workout.exercises[nextIndex],
                    completedSets = emptyList(),
                    upcomingExercises = workout.exercises.drop(nextIndex + 1),
                    exerciseLogs = exerciseLogs,
                    isResting = false
                )
            }
        }
    }
    
    fun finishWorkout() {
        timerJob?.cancel()
        restTimerJob?.cancel()
        
        val workout = _uiState.value.workout ?: return
        val endTime = System.currentTimeMillis()
        
        // Convert exercise logs to the format needed
        val exerciseLogList = _uiState.value.exerciseLogs.map { (exerciseId, sets) ->
            val exercise = workout.exercises.find { it.exercise.id == exerciseId }
            ExerciseLog(
                exerciseId = exerciseId,
                exerciseName = exercise?.exercise?.name ?: "Unknown",
                sets = sets.toList()
            )
        }
        
        val workoutLog = WorkoutLog(
            id = UUID.randomUUID().toString(),
            workoutId = workout.id,
            workoutName = workout.name,
            startTime = startTime,
            endTime = endTime,
            exerciseLogs = exerciseLogList
        )
        
        viewModelScope.launch {
            workoutLogRepository.saveLog(workoutLog)
            _uiState.update { it.copy(isComplete = true) }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        restTimerJob?.cancel()
    }
}
