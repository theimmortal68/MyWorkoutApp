package com.workoutgen.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutgen.domain.model.Workout
import com.workoutgen.domain.model.WorkoutLog
import com.workoutgen.domain.repository.WorkoutLogRepository
import com.workoutgen.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class HomeUiState(
    val recentWorkouts: List<Workout> = emptyList(),
    val favoriteWorkouts: List<Workout> = emptyList(),
    val recentLogs: List<WorkoutLog> = emptyList(),
    val workoutsThisWeek: Int = 0,
    val totalMinutesThisWeek: Int = 0,
    val currentStreak: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val workoutLogRepository: WorkoutLogRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            // Load recent workouts
            workoutRepository.getAllWorkouts()
                .collect { workouts ->
                    _uiState.update { state ->
                        state.copy(
                            recentWorkouts = workouts.take(5),
                            isLoading = false
                        )
                    }
                }
        }
        
        viewModelScope.launch {
            // Load favorite workouts
            workoutRepository.getFavoriteWorkouts()
                .collect { favorites ->
                    _uiState.update { state ->
                        state.copy(favoriteWorkouts = favorites)
                    }
                }
        }
        
        viewModelScope.launch {
            // Load recent logs and calculate stats
            workoutLogRepository.getRecentLogs(30)
                .collect { logs ->
                    val stats = calculateWeeklyStats(logs)
                    _uiState.update { state ->
                        state.copy(
                            recentLogs = logs.take(5),
                            workoutsThisWeek = stats.workoutsThisWeek,
                            totalMinutesThisWeek = stats.totalMinutes,
                            currentStreak = stats.currentStreak
                        )
                    }
                }
        }
    }
    
    private fun calculateWeeklyStats(logs: List<WorkoutLog>): WeeklyStats {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val weekStart = calendar.timeInMillis
        
        val thisWeekLogs = logs.filter { it.startTime >= weekStart }
        
        val workoutsThisWeek = thisWeekLogs.size
        val totalMinutes = thisWeekLogs.sumOf { log ->
            val duration = (log.endTime ?: log.startTime) - log.startTime
            (duration / 60000).toInt()  // Convert ms to minutes
        }
        
        // Calculate streak (consecutive days with workouts)
        val streak = calculateStreak(logs)
        
        return WeeklyStats(workoutsThisWeek, totalMinutes, streak)
    }
    
    private fun calculateStreak(logs: List<WorkoutLog>): Int {
        if (logs.isEmpty()) return 0
        
        val calendar = Calendar.getInstance()
        var streak = 0
        var currentDay = calendar.get(Calendar.DAY_OF_YEAR)
        var currentYear = calendar.get(Calendar.YEAR)
        
        val sortedLogs = logs.sortedByDescending { it.startTime }
        
        for (log in sortedLogs) {
            calendar.timeInMillis = log.startTime
            val logDay = calendar.get(Calendar.DAY_OF_YEAR)
            val logYear = calendar.get(Calendar.YEAR)
            
            val dayDiff = if (logYear == currentYear) {
                currentDay - logDay
            } else {
                // Simplified - doesn't handle year boundaries perfectly
                break
            }
            
            when (dayDiff) {
                0 -> {
                    if (streak == 0) streak = 1
                }
                1 -> {
                    streak++
                    currentDay = logDay
                }
                else -> break
            }
        }
        
        return streak
    }
    
    private data class WeeklyStats(
        val workoutsThisWeek: Int,
        val totalMinutes: Int,
        val currentStreak: Int
    )
}
