package com.workoutgen.data.local.dao

import androidx.room.*
import com.workoutgen.data.local.entity.*
import com.workoutgen.domain.model.Difficulty
import com.workoutgen.domain.model.MuscleGroup
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    
    @Query("SELECT * FROM exercises")
    fun getAllExercises(): Flow<List<ExerciseEntity>>
    
    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: String): ExerciseEntity?
    
    @Query("SELECT * FROM exercises WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchExercises(query: String): Flow<List<ExerciseEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<ExerciseEntity>)
    
    @Delete
    suspend fun deleteExercise(exercise: ExerciseEntity)
    
    @Query("DELETE FROM exercises")
    suspend fun deleteAllExercises()
}

@Dao
interface WorkoutDao {
    
    @Query("SELECT * FROM workouts ORDER BY createdAt DESC")
    fun getAllWorkouts(): Flow<List<WorkoutEntity>>
    
    @Query("SELECT * FROM workouts WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteWorkouts(): Flow<List<WorkoutEntity>>
    
    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getWorkoutById(id: String): WorkoutEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutEntity)
    
    @Update
    suspend fun updateWorkout(workout: WorkoutEntity)
    
    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkout(id: String)
    
    @Query("UPDATE workouts SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: String)
}

@Dao
interface WorkoutLogDao {
    
    @Query("SELECT * FROM workout_logs ORDER BY startTime DESC")
    fun getAllLogs(): Flow<List<WorkoutLogEntity>>
    
    @Query("SELECT * FROM workout_logs WHERE workoutId = :workoutId ORDER BY startTime DESC")
    fun getLogsForWorkout(workoutId: String): Flow<List<WorkoutLogEntity>>
    
    @Query("SELECT * FROM workout_logs ORDER BY startTime DESC LIMIT :limit")
    fun getRecentLogs(limit: Int): Flow<List<WorkoutLogEntity>>
    
    @Query("SELECT * FROM workout_logs WHERE startTime >= :startTime AND startTime <= :endTime ORDER BY startTime DESC")
    fun getLogsByDateRange(startTime: Long, endTime: Long): Flow<List<WorkoutLogEntity>>
    
    @Query("SELECT * FROM workout_logs WHERE id = :id")
    suspend fun getLogById(id: String): WorkoutLogEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: WorkoutLogEntity)
    
    @Update
    suspend fun updateLog(log: WorkoutLogEntity)
    
    @Query("DELETE FROM workout_logs WHERE id = :id")
    suspend fun deleteLog(id: String)
}

@Dao
interface UserProfileDao {
    
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfileEntity)
    
    @Update
    suspend fun updateProfile(profile: UserProfileEntity)
}
