package com.workoutgen.domain.repository

import com.workoutgen.domain.model.*
import com.workoutgen.domain.usecase.ExercisePerformance
import kotlinx.coroutines.flow.Flow

/**
 * Repository for exercise data - the exercise library
 */
interface ExerciseRepository {
    
    fun getAllExercises(): Flow<List<Exercise>>
    
    suspend fun getExerciseById(id: String): Exercise?
    
    fun getExercisesByMuscleGroup(muscleGroup: MuscleGroup): Flow<List<Exercise>>
    
    fun getExercisesByEquipment(equipment: Set<Equipment>): Flow<List<Exercise>>
    
    fun getExercisesByDifficulty(maxDifficulty: Difficulty): Flow<List<Exercise>>
    
    fun searchExercises(query: String): Flow<List<Exercise>>
    
    fun getFilteredExercises(
        muscleGroups: List<MuscleGroup>? = null,
        equipment: Set<Equipment>? = null,
        maxDifficulty: Difficulty? = null,
        types: List<ExerciseType>? = null,
        excludeIds: List<String> = emptyList()
    ): Flow<List<Exercise>>
}

/**
 * Repository for generated and saved workouts
 */
interface WorkoutRepository {
    
    fun getAllWorkouts(): Flow<List<Workout>>
    
    fun getFavoriteWorkouts(): Flow<List<Workout>>
    
    suspend fun getWorkoutById(id: String): Workout?
    
    suspend fun saveWorkout(workout: Workout)
    
    suspend fun deleteWorkout(id: String)
    
    suspend fun updateWorkout(workout: Workout)
    
    suspend fun toggleFavorite(id: String)
}

/**
 * Repository for workout history/logs
 */
interface WorkoutLogRepository {
    
    fun getAllLogs(): Flow<List<WorkoutLog>>
    
    fun getLogsForWorkout(workoutId: String): Flow<List<WorkoutLog>>
    
    fun getRecentLogs(limit: Int = 10): Flow<List<WorkoutLog>>

    suspend fun getLogsFromRecentDays(days: Int): List<WorkoutLog>
    
    fun getLogsByDateRange(startTime: Long, endTime: Long): Flow<List<WorkoutLog>>
    
    suspend fun getLogById(id: String): WorkoutLog?
    
    suspend fun saveLog(log: WorkoutLog)
    
    suspend fun updateLog(log: WorkoutLog)
    
    suspend fun deleteLog(id: String)
    
    suspend fun getLastPerformance(exerciseId: String): ExerciseLog?
    
    suspend fun getPersonalRecords(exerciseId: String): PersonalRecords?
    
    /**
     * Get all personal records across all exercises
     */
    suspend fun getPersonalRecords(): List<PersonalRecord>
    
    /**
     * Get performance history for all exercises (for AI progression)
     */
    suspend fun getExercisePerformanceHistory(): Map<String, List<ExercisePerformance>>
}

data class PersonalRecords(
    val exerciseId: String,
    val maxWeight: Float?,
    val maxReps: Int?,
    val maxVolume: Float?,
    val oneRepMaxEstimate: Float?
)

/**
 * Repository for user profile and preferences
 */
interface UserProfileRepository {
    
    fun getUserProfile(): Flow<UserProfile?>
    
    fun getProfile(): Flow<UserProfile?>
    
    suspend fun updateProfile(profile: UserProfile)
    
    suspend fun updateEquipment(equipment: Set<Equipment>)
    
    suspend fun updateGoals(goals: List<FitnessGoal>)
    
    suspend fun updateExperienceLevel(level: Difficulty)
    
    suspend fun addExcludedExercise(exerciseId: String)
    
    suspend fun removeExcludedExercise(exerciseId: String)
}
