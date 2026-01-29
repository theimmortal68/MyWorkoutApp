package com.workoutgen.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.workoutgen.domain.model.*

// ============================================================================
// TYPE CONVERTERS - For storing complex types in Room
// ============================================================================

class Converters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromStringList(value: List<String>): String = gson.toJson(value)
    
    @TypeConverter
    fun toStringList(value: String): List<String> =
        gson.fromJson(value, object : TypeToken<List<String>>() {}.type)
    
    @TypeConverter
    fun fromMuscleGroupList(value: List<MuscleGroup>): String = gson.toJson(value.map { it.name })
    
    @TypeConverter
    fun toMuscleGroupList(value: String): List<MuscleGroup> {
        val names: List<String> = gson.fromJson(value, object : TypeToken<List<String>>() {}.type)
        return names.map { MuscleGroup.valueOf(it) }
    }
    
    @TypeConverter
    fun fromEquipmentList(value: List<Equipment>): String = gson.toJson(value.map { it.name })
    
    @TypeConverter
    fun toEquipmentList(value: String): List<Equipment> {
        val names: List<String> = gson.fromJson(value, object : TypeToken<List<String>>() {}.type)
        return names.map { Equipment.valueOf(it) }
    }
    
    @TypeConverter
    fun fromEquipmentSet(value: Set<Equipment>): String = gson.toJson(value.map { it.name })
    
    @TypeConverter
    fun toEquipmentSet(value: String): Set<Equipment> {
        val names: List<String> = gson.fromJson(value, object : TypeToken<List<String>>() {}.type)
        return names.map { Equipment.valueOf(it) }.toSet()
    }
    
    @TypeConverter
    fun fromFitnessGoalList(value: List<FitnessGoal>): String = gson.toJson(value.map { it.name })
    
    @TypeConverter
    fun toFitnessGoalList(value: String): List<FitnessGoal> {
        val names: List<String> = gson.fromJson(value, object : TypeToken<List<String>>() {}.type)
        return names.map { FitnessGoal.valueOf(it) }
    }
    
    @TypeConverter
    fun fromExerciseType(value: ExerciseType): String = value.name
    
    @TypeConverter
    fun toExerciseType(value: String): ExerciseType = ExerciseType.valueOf(value)
    
    @TypeConverter
    fun fromDifficulty(value: Difficulty): String = value.name
    
    @TypeConverter
    fun toDifficulty(value: String): Difficulty = Difficulty.valueOf(value)
    
    @TypeConverter
    fun fromFitnessGoal(value: FitnessGoal): String = value.name
    
    @TypeConverter
    fun toFitnessGoal(value: String): FitnessGoal = FitnessGoal.valueOf(value)
    
    @TypeConverter
    fun fromWorkoutSplit(value: WorkoutSplit): String = value.name
    
    @TypeConverter
    fun toWorkoutSplit(value: String): WorkoutSplit = WorkoutSplit.valueOf(value)
    
    @TypeConverter
    fun fromWorkoutExerciseList(value: List<WorkoutExercise>): String = gson.toJson(value)
    
    @TypeConverter
    fun toWorkoutExerciseList(value: String): List<WorkoutExercise> =
        gson.fromJson(value, object : TypeToken<List<WorkoutExercise>>() {}.type)
    
    @TypeConverter
    fun fromExerciseLogList(value: List<ExerciseLog>): String = gson.toJson(value)

    @TypeConverter
    fun toExerciseLogList(value: String): List<ExerciseLog> =
        gson.fromJson(value, object : TypeToken<List<ExerciseLog>>() {}.type)

    @TypeConverter
    fun fromUserLimitationList(value: List<UserLimitation>): String = gson.toJson(value)

    @TypeConverter
    fun toUserLimitationList(value: String): List<UserLimitation> =
        gson.fromJson(value, object : TypeToken<List<UserLimitation>>() {}.type) ?: emptyList()
}

// ============================================================================
// ROOM ENTITIES
// ============================================================================

@Entity(tableName = "exercises")
@TypeConverters(Converters::class)
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val instructions: List<String>,
    val primaryMuscles: List<MuscleGroup>,
    val secondaryMuscles: List<MuscleGroup>,
    val equipmentRequired: List<Equipment>,
    val type: ExerciseType,
    val difficulty: Difficulty,
    val videoUrl: String?,
    val imageUrl: String?,
    val tips: List<String>,
    val alternatives: List<String>
)

@Entity(tableName = "workouts")
@TypeConverters(Converters::class)
data class WorkoutEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val exercises: List<WorkoutExercise>,  // Stored as JSON
    val targetMuscles: List<MuscleGroup>,
    val estimatedDurationMinutes: Int,
    val difficulty: Difficulty,
    val goal: FitnessGoal,
    val warmup: List<WorkoutExercise>,
    val cooldown: List<WorkoutExercise>,
    val createdAt: Long,
    val isFavorite: Boolean
)

@Entity(tableName = "workout_logs")
@TypeConverters(Converters::class)
data class WorkoutLogEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val workoutName: String,
    val startTime: Long,
    val endTime: Long?,
    val exerciseLogs: List<ExerciseLog>,  // Stored as JSON
    val notes: String?,
    val rating: Int?,
    val mood: String?
)

@Entity(tableName = "user_profile")
@TypeConverters(Converters::class)
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val experienceLevel: Difficulty,
    val fitnessGoals: List<FitnessGoal>,
    val availableEquipment: Set<Equipment>,
    val preferredWorkoutDuration: Int,
    val workoutDaysPerWeek: Int,
    val preferredSplit: WorkoutSplit,
    val physicalLimitations: List<UserLimitation>,
    val excludedExercises: List<String>,
    val useMetricUnits: Boolean
)

// ============================================================================
// ENTITY MAPPERS
// ============================================================================

fun ExerciseEntity.toDomain() = Exercise(
    id = id,
    name = name,
    description = description,
    instructions = instructions,
    primaryMuscles = primaryMuscles,
    secondaryMuscles = secondaryMuscles,
    equipmentRequired = equipmentRequired,
    type = type,
    difficulty = difficulty,
    videoUrl = videoUrl,
    imageUrl = imageUrl,
    tips = tips,
    alternatives = alternatives
)

fun Exercise.toEntity() = ExerciseEntity(
    id = id,
    name = name,
    description = description,
    instructions = instructions,
    primaryMuscles = primaryMuscles,
    secondaryMuscles = secondaryMuscles,
    equipmentRequired = equipmentRequired,
    type = type,
    difficulty = difficulty,
    videoUrl = videoUrl,
    imageUrl = imageUrl,
    tips = tips,
    alternatives = alternatives
)

fun WorkoutEntity.toDomain() = Workout(
    id = id,
    name = name,
    description = description,
    exercises = exercises,
    targetMuscles = targetMuscles,
    estimatedDurationMinutes = estimatedDurationMinutes,
    difficulty = difficulty,
    goal = goal,
    warmup = warmup,
    cooldown = cooldown,
    createdAt = createdAt,
    isFavorite = isFavorite
)

fun Workout.toEntity() = WorkoutEntity(
    id = id,
    name = name,
    description = description,
    exercises = exercises,
    targetMuscles = targetMuscles,
    estimatedDurationMinutes = estimatedDurationMinutes,
    difficulty = difficulty,
    goal = goal,
    warmup = warmup,
    cooldown = cooldown,
    createdAt = createdAt,
    isFavorite = isFavorite
)

fun WorkoutLogEntity.toDomain() = WorkoutLog(
    id = id,
    workoutId = workoutId,
    workoutName = workoutName,
    startTime = startTime,
    endTime = endTime,
    exerciseLogs = exerciseLogs,
    notes = notes,
    rating = rating,
    mood = mood
)

fun WorkoutLog.toEntity() = WorkoutLogEntity(
    id = id,
    workoutId = workoutId,
    workoutName = workoutName,
    startTime = startTime,
    endTime = endTime,
    exerciseLogs = exerciseLogs,
    notes = notes,
    rating = rating,
    mood = mood
)

fun UserProfileEntity.toDomain() = UserProfile(
    id = id,
    name = name,
    experienceLevel = experienceLevel,
    fitnessGoals = fitnessGoals,
    availableEquipment = availableEquipment,
    preferredWorkoutDuration = preferredWorkoutDuration,
    workoutDaysPerWeek = workoutDaysPerWeek,
    preferredSplit = preferredSplit,
    physicalLimitations = physicalLimitations,
    excludedExercises = excludedExercises,
    useMetricUnits = useMetricUnits
)

fun UserProfile.toEntity() = UserProfileEntity(
    id = id,
    name = name,
    experienceLevel = experienceLevel,
    fitnessGoals = fitnessGoals,
    availableEquipment = availableEquipment,
    preferredWorkoutDuration = preferredWorkoutDuration,
    workoutDaysPerWeek = workoutDaysPerWeek,
    preferredSplit = preferredSplit,
    physicalLimitations = physicalLimitations,
    excludedExercises = excludedExercises,
    useMetricUnits = useMetricUnits
)
