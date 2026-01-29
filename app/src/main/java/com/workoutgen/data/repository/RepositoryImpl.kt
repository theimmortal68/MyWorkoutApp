package com.workoutgen.data.repository

import com.workoutgen.data.local.dao.*
import com.workoutgen.data.local.entity.*
import com.workoutgen.domain.model.*
import com.workoutgen.domain.repository.*
import com.workoutgen.domain.usecase.ExercisePerformance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRepositoryImpl @Inject constructor(
    private val exerciseDao: ExerciseDao
) : ExerciseRepository {
    
    override fun getAllExercises(): Flow<List<Exercise>> =
        exerciseDao.getAllExercises().map { entities ->
            entities.map { it.toDomain() }
        }
    
    override suspend fun getExerciseById(id: String): Exercise? =
        exerciseDao.getExerciseById(id)?.toDomain()
    
    override fun getExercisesByMuscleGroup(muscleGroup: MuscleGroup): Flow<List<Exercise>> =
        getAllExercises().map { exercises ->
            exercises.filter { muscleGroup in it.primaryMuscles }
        }
    
    override fun getExercisesByEquipment(equipment: Set<Equipment>): Flow<List<Exercise>> =
        getAllExercises().map { exercises ->
            exercises.filter { exercise ->
                exercise.equipmentRequired.all { it in equipment } ||
                exercise.equipmentRequired.isEmpty()
            }
        }
    
    override fun getExercisesByDifficulty(maxDifficulty: Difficulty): Flow<List<Exercise>> =
        getAllExercises().map { exercises ->
            exercises.filter { it.difficulty.ordinal <= maxDifficulty.ordinal }
        }
    
    override fun searchExercises(query: String): Flow<List<Exercise>> =
        exerciseDao.searchExercises(query).map { entities ->
            entities.map { it.toDomain() }
        }
    
    override fun getFilteredExercises(
        muscleGroups: List<MuscleGroup>?,
        equipment: Set<Equipment>?,
        maxDifficulty: Difficulty?,
        types: List<ExerciseType>?,
        excludeIds: List<String>
    ): Flow<List<Exercise>> = getAllExercises().map { exercises ->
        exercises.filter { exercise ->
            // Filter by muscle groups
            (muscleGroups == null || exercise.primaryMuscles.any { it in muscleGroups }) &&
            // Filter by equipment (user has required equipment OR exercise needs no equipment)
            (equipment == null || 
                exercise.equipmentRequired.all { it in equipment } ||
                exercise.equipmentRequired.isEmpty() ||
                exercise.equipmentRequired == listOf(Equipment.NONE)) &&
            // Filter by difficulty
            (maxDifficulty == null || exercise.difficulty.ordinal <= maxDifficulty.ordinal) &&
            // Filter by type
            (types == null || exercise.type in types) &&
            // Exclude specific exercises
            exercise.id !in excludeIds
        }
    }
}

@Singleton
class WorkoutRepositoryImpl @Inject constructor(
    private val workoutDao: WorkoutDao
) : WorkoutRepository {
    
    override fun getAllWorkouts(): Flow<List<Workout>> =
        workoutDao.getAllWorkouts().map { entities ->
            entities.map { it.toDomain() }
        }
    
    override fun getFavoriteWorkouts(): Flow<List<Workout>> =
        workoutDao.getFavoriteWorkouts().map { entities ->
            entities.map { it.toDomain() }
        }
    
    override suspend fun getWorkoutById(id: String): Workout? =
        workoutDao.getWorkoutById(id)?.toDomain()
    
    override suspend fun saveWorkout(workout: Workout) {
        workoutDao.insertWorkout(workout.toEntity())
    }
    
    override suspend fun deleteWorkout(id: String) {
        workoutDao.deleteWorkout(id)
    }
    
    override suspend fun updateWorkout(workout: Workout) {
        workoutDao.updateWorkout(workout.toEntity())
    }
    
    override suspend fun toggleFavorite(id: String) {
        workoutDao.toggleFavorite(id)
    }
}

@Singleton
class WorkoutLogRepositoryImpl @Inject constructor(
    private val workoutLogDao: WorkoutLogDao
) : WorkoutLogRepository {
    
    override fun getAllLogs(): Flow<List<WorkoutLog>> =
        workoutLogDao.getAllLogs().map { entities ->
            entities.map { it.toDomain() }
        }
    
    override fun getLogsForWorkout(workoutId: String): Flow<List<WorkoutLog>> =
        workoutLogDao.getLogsForWorkout(workoutId).map { entities ->
            entities.map { it.toDomain() }
        }
    
    override fun getRecentLogs(limit: Int): Flow<List<WorkoutLog>> =
        workoutLogDao.getRecentLogs(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    
    override fun getLogsByDateRange(startTime: Long, endTime: Long): Flow<List<WorkoutLog>> =
        workoutLogDao.getLogsByDateRange(startTime, endTime).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getLogsFromRecentDays(days: Int): List<WorkoutLog> {
        val now = System.currentTimeMillis()
        val startTime = now - (days.toLong() * 24 * 60 * 60 * 1000)
        return getLogsByDateRange(startTime, now).first()
    }

    override suspend fun getLogById(id: String): WorkoutLog? =
        workoutLogDao.getLogById(id)?.toDomain()
    
    override suspend fun saveLog(log: WorkoutLog) {
        workoutLogDao.insertLog(log.toEntity())
    }
    
    override suspend fun updateLog(log: WorkoutLog) {
        workoutLogDao.updateLog(log.toEntity())
    }
    
    override suspend fun deleteLog(id: String) {
        workoutLogDao.deleteLog(id)
    }
    
    override suspend fun getLastPerformance(exerciseId: String): ExerciseLog? {
        return getAllLogs().first()
            .flatMap { it.exerciseLogs }
            .lastOrNull { it.exerciseId == exerciseId }
    }
    
    override suspend fun getPersonalRecords(exerciseId: String): PersonalRecords? {
        val allSets = getAllLogs().first()
            .flatMap { it.exerciseLogs }
            .filter { it.exerciseId == exerciseId }
            .flatMap { it.sets }
            .filterNot { it.isWarmup }
        
        if (allSets.isEmpty()) return null
        
        val maxWeight = allSets.mapNotNull { it.weight }.maxOrNull()
        val maxReps = allSets.maxOfOrNull { it.reps }
        val maxVolume = allSets.mapNotNull { set ->
            set.weight?.let { it * set.reps }
        }.maxOrNull()
        
        // Brzycki formula for 1RM estimate
        val oneRepMax = allSets.mapNotNull { set ->
            set.weight?.let { weight ->
                if (set.reps in 1..10) {
                    weight * (36f / (37f - set.reps))
                } else null
            }
        }.maxOrNull()
        
        return PersonalRecords(
            exerciseId = exerciseId,
            maxWeight = maxWeight,
            maxReps = maxReps,
            maxVolume = maxVolume,
            oneRepMaxEstimate = oneRepMax
        )
    }

    override suspend fun getPersonalRecords(): List<PersonalRecord> {
        val allLogs = getAllLogs().first()
        val exerciseIds = allLogs.flatMap { it.exerciseLogs }.map { it.exerciseId }.distinct()

        return exerciseIds.mapNotNull { exerciseId ->
            val records = getPersonalRecords(exerciseId)
            records?.let {
                PersonalRecord(
                    exerciseId = exerciseId,
                    exerciseName = exerciseId,
                    value = it.maxWeight ?: 0f,
                    reps = it.maxReps,
                    date = System.currentTimeMillis(),
                    type = PRType.WEIGHT
                )
            }
        }
    }

    override suspend fun getExercisePerformanceHistory(): Map<String, List<ExercisePerformance>> {
        val allLogs = getAllLogs().first()
        val result = mutableMapOf<String, MutableList<ExercisePerformance>>()

        allLogs.forEach { log ->
            log.exerciseLogs.forEach { exerciseLog ->
                val performances = result.getOrPut(exerciseLog.exerciseId) { mutableListOf() }

                exerciseLog.sets.filterNot { it.isWarmup }.forEach { set ->
                    performances.add(
                        ExercisePerformance(
                            exerciseId = exerciseLog.exerciseId,
                            date = log.startTime,
                            weight = set.weight ?: 0f,
                            targetReps = set.reps,
                            achievedReps = set.reps,
                            sets = 1,
                            completedAllSets = set.reps > 0,
                            rpe = set.rpe
                        )
                    )
                }
            }
        }

        return result
    }
}

@Singleton
class UserProfileRepositoryImpl @Inject constructor(
    private val userProfileDao: UserProfileDao
) : UserProfileRepository {
    
    override fun getUserProfile(): Flow<UserProfile?> =
        userProfileDao.getUserProfile().map { entity ->
            entity?.toDomain() ?: UserProfile()
        }

    override fun getProfile(): Flow<UserProfile?> = getUserProfile()

    override suspend fun updateProfile(profile: UserProfile) {
        userProfileDao.insertProfile(profile.toEntity())
    }
    
    override suspend fun updateEquipment(equipment: Set<Equipment>) {
        val current = userProfileDao.getUserProfile().first()?.toDomain() ?: UserProfile()
        updateProfile(current.copy(availableEquipment = equipment))
    }
    
    override suspend fun updateGoals(goals: List<FitnessGoal>) {
        val current = userProfileDao.getUserProfile().first()?.toDomain() ?: UserProfile()
        updateProfile(current.copy(fitnessGoals = goals))
    }
    
    override suspend fun updateExperienceLevel(level: Difficulty) {
        val current = userProfileDao.getUserProfile().first()?.toDomain() ?: UserProfile()
        updateProfile(current.copy(experienceLevel = level))
    }
    
    override suspend fun addExcludedExercise(exerciseId: String) {
        val current = userProfileDao.getUserProfile().first()?.toDomain() ?: UserProfile()
        if (exerciseId !in current.excludedExercises) {
            updateProfile(current.copy(excludedExercises = current.excludedExercises + exerciseId))
        }
    }
    
    override suspend fun removeExcludedExercise(exerciseId: String) {
        val current = userProfileDao.getUserProfile().first()?.toDomain() ?: UserProfile()
        updateProfile(current.copy(excludedExercises = current.excludedExercises - exerciseId))
    }
}
