package com.workoutgen.domain.usecase

import com.workoutgen.domain.model.*
import com.workoutgen.domain.repository.ExerciseRepository
import com.workoutgen.domain.repository.WorkoutLogRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.random.Random

/**
 * Core use case for generating dynamic workouts based on user parameters
 */
class GenerateWorkoutUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val workoutLogRepository: WorkoutLogRepository,
    private val physicalLimitationFilter: PhysicalLimitationFilter
) {
    
    suspend operator fun invoke(params: WorkoutGenerationParams): Result<Workout> {
        return try {
            val workout = generateWorkout(params)
            Result.success(workout)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun generateWorkout(params: WorkoutGenerationParams): Workout {
        val profile = params.userProfile
        val duration = params.durationMinutes ?: profile.preferredWorkoutDuration
        
        // Determine target muscles based on split or explicit selection
        val targetMuscles = params.targetMuscles ?: selectMusclesForSplit(profile.preferredSplit)
        
        // Get available exercises based on equipment and difficulty
        val filteredByEquipment = exerciseRepository.getFilteredExercises(
            muscleGroups = targetMuscles,
            equipment = profile.availableEquipment,
            maxDifficulty = profile.experienceLevel,
            excludeIds = profile.excludedExercises
        ).first()

        // Apply physical limitation filter
        val availableExercises = physicalLimitationFilter.filterExercises(
            exercises = filteredByEquipment,
            limitations = profile.physicalLimitations
        )

        if (availableExercises.isEmpty()) {
            throw NoExercisesAvailableException(
                "No exercises found matching your equipment and experience level"
            )
        }
        
        // Select exercises based on goal
        val selectedExercises = selectExercises(
            available = availableExercises,
            targetMuscles = targetMuscles,
            goal = profile.fitnessGoals.firstOrNull() ?: FitnessGoal.GENERAL_FITNESS,
            maxExercises = params.maxExercises,
            prioritizeCompound = params.prioritizeCompound
        )
        
        // Create workout exercises with appropriate set/rep schemes
        val workoutExercises = selectedExercises.map { exercise ->
            createWorkoutExercise(
                exercise = exercise,
                goal = profile.fitnessGoals.firstOrNull() ?: FitnessGoal.GENERAL_FITNESS,
                experienceLevel = profile.experienceLevel
            )
        }
        
        // Generate warmup and cooldown if requested
        val warmup = if (params.includeWarmup) generateWarmup(targetMuscles) else emptyList()
        val cooldown = if (params.includeCooldown) generateCooldown() else emptyList()
        
        // Calculate estimated duration
        val exerciseDuration = workoutExercises.sumOf { ex ->
            val timePerSet = (ex.reps.last * 3) + ex.restSeconds  // ~3 sec per rep + rest
            ex.sets * timePerSet
        } / 60  // Convert to minutes
        
        val warmupDuration = if (params.includeWarmup) 5 else 0
        val cooldownDuration = if (params.includeCooldown) 5 else 0
        val totalDuration = exerciseDuration + warmupDuration + cooldownDuration
        
        return Workout(
            name = generateWorkoutName(targetMuscles, profile.fitnessGoals.firstOrNull()),
            description = generateDescription(targetMuscles, profile.fitnessGoals.firstOrNull()),
            exercises = workoutExercises,
            targetMuscles = targetMuscles,
            estimatedDurationMinutes = totalDuration,
            difficulty = profile.experienceLevel,
            goal = profile.fitnessGoals.firstOrNull() ?: FitnessGoal.GENERAL_FITNESS,
            warmup = warmup,
            cooldown = cooldown
        )
    }
    
    /**
     * Select which muscle groups to target based on workout split
     */
    private fun selectMusclesForSplit(split: WorkoutSplit): List<MuscleGroup> {
        return when (split) {
            WorkoutSplit.FULL_BODY -> listOf(
                MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS,
                MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.CORE
            )
            WorkoutSplit.UPPER_LOWER -> {
                // Alternate - for simplicity, randomly choose
                if (Random.nextBoolean()) {
                    listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS,
                        MuscleGroup.BICEPS, MuscleGroup.TRICEPS)
                } else {
                    listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS,
                        MuscleGroup.GLUTES, MuscleGroup.CALVES, MuscleGroup.CORE)
                }
            }
            WorkoutSplit.PUSH_PULL_LEGS -> {
                when (Random.nextInt(3)) {
                    0 -> listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS)  // Push
                    1 -> listOf(MuscleGroup.BACK, MuscleGroup.BICEPS, MuscleGroup.FOREARMS)     // Pull
                    else -> listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS,
                        MuscleGroup.GLUTES, MuscleGroup.CALVES)  // Legs
                }
            }
            WorkoutSplit.BRO_SPLIT -> {
                listOf(MuscleGroup.entries[Random.nextInt(MuscleGroup.entries.size - 1)])  // Exclude FULL_BODY
            }
            WorkoutSplit.ARNOLD_SPLIT -> {
                when (Random.nextInt(3)) {
                    0 -> listOf(MuscleGroup.CHEST, MuscleGroup.BACK)
                    1 -> listOf(MuscleGroup.SHOULDERS, MuscleGroup.BICEPS, MuscleGroup.TRICEPS)
                    else -> listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES)
                }
            }
            WorkoutSplit.POWERLIFTING -> listOf(MuscleGroup.QUADRICEPS, MuscleGroup.CHEST, MuscleGroup.BACK)
            WorkoutSplit.CUSTOM -> listOf(MuscleGroup.FULL_BODY)
        }
    }
    
    /**
     * Select exercises to include in the workout
     */
    private fun selectExercises(
        available: List<Exercise>,
        targetMuscles: List<MuscleGroup>,
        goal: FitnessGoal,
        maxExercises: Int,
        prioritizeCompound: Boolean
    ): List<Exercise> {
        val selected = mutableListOf<Exercise>()
        val musclesCovered = mutableSetOf<MuscleGroup>()
        
        // Group exercises by primary muscle
        val byMuscle = available.groupBy { it.primaryMuscles.firstOrNull() }
        
        // First pass: Select compound exercises if prioritizing
        if (prioritizeCompound) {
            val compounds = available.filter { it.type == ExerciseType.COMPOUND }
            compounds.shuffled().take(maxExercises / 2).forEach { exercise ->
                if (selected.size < maxExercises) {
                    selected.add(exercise)
                    musclesCovered.addAll(exercise.primaryMuscles)
                    musclesCovered.addAll(exercise.secondaryMuscles)
                }
            }
        }
        
        // Second pass: Fill in gaps for target muscles
        for (muscle in targetMuscles) {
            if (muscle !in musclesCovered && selected.size < maxExercises) {
                val exercisesForMuscle = byMuscle[muscle]?.filterNot { it in selected } ?: continue
                exercisesForMuscle.randomOrNull()?.let { exercise ->
                    selected.add(exercise)
                    musclesCovered.addAll(exercise.primaryMuscles)
                }
            }
        }
        
        // Third pass: Fill remaining slots based on goal
        while (selected.size < maxExercises) {
            val remaining = available.filterNot { it in selected }
            if (remaining.isEmpty()) break
            
            val exercise = when (goal) {
                FitnessGoal.BUILD_MUSCLE -> remaining.filter { 
                    it.type == ExerciseType.COMPOUND || it.type == ExerciseType.ISOLATION 
                }.randomOrNull()
                FitnessGoal.BUILD_STRENGTH -> remaining.filter { 
                    it.type == ExerciseType.COMPOUND 
                }.randomOrNull()
                FitnessGoal.LOSE_WEIGHT -> remaining.filter { 
                    it.type == ExerciseType.COMPOUND || it.type == ExerciseType.CARDIO 
                }.randomOrNull()
                FitnessGoal.IMPROVE_ENDURANCE -> remaining.filter { 
                    it.type == ExerciseType.CARDIO || it.type == ExerciseType.PLYOMETRIC 
                }.randomOrNull()
                FitnessGoal.INCREASE_FLEXIBILITY -> remaining.filter { 
                    it.type == ExerciseType.STRETCHING 
                }.randomOrNull()
                else -> remaining.randomOrNull()
            } ?: remaining.randomOrNull() ?: break
            
            selected.add(exercise)
        }
        
        return selected
    }
    
    /**
     * Create a WorkoutExercise with appropriate set/rep scheme based on goal
     */
    private suspend fun createWorkoutExercise(
        exercise: Exercise,
        goal: FitnessGoal,
        experienceLevel: Difficulty
    ): WorkoutExercise {
        // Get last performance for progressive overload hints
        val lastPerformance = workoutLogRepository.getLastPerformance(exercise.id)
        
        val (sets, reps, rest) = when (goal) {
            FitnessGoal.BUILD_STRENGTH -> Triple(5, 3..5, 180)      // Heavy, low reps, long rest
            FitnessGoal.BUILD_MUSCLE -> Triple(4, 8..12, 90)        // Moderate weight, moderate reps
            FitnessGoal.LOSE_WEIGHT -> Triple(3, 12..15, 45)        // Light weight, high reps, short rest
            FitnessGoal.IMPROVE_ENDURANCE -> Triple(3, 15..20, 30)  // Very light, very high reps
            FitnessGoal.INCREASE_FLEXIBILITY -> Triple(3, 1..1, 60) // Holds
            FitnessGoal.ATHLETIC_PERFORMANCE -> Triple(4, 6..8, 120)
            FitnessGoal.POWERBUILDING -> Triple(4, 5..8, 150)       // Mix of strength and hypertrophy
            FitnessGoal.GENERAL_FITNESS -> Triple(3, 10..12, 60)
        }
        
        // Adjust sets based on experience
        val adjustedSets = when (experienceLevel) {
            Difficulty.BEGINNER -> maxOf(2, sets - 1)
            Difficulty.INTERMEDIATE -> sets
            Difficulty.ADVANCED -> sets + 1
            Difficulty.EXPERT -> sets + 2
        }
        
        // Suggest weight based on last performance
        val suggestedWeight = lastPerformance?.sets?.lastOrNull()?.weight?.let { lastWeight ->
            // Suggest slight increase for progressive overload
            lastWeight * 1.025f
        }
        
        return WorkoutExercise(
            exercise = exercise,
            sets = adjustedSets,
            reps = reps,
            restSeconds = rest,
            weight = suggestedWeight,
            rpe = when (goal) {
                FitnessGoal.BUILD_STRENGTH -> 9
                FitnessGoal.BUILD_MUSCLE -> 8
                else -> 7
            }
        )
    }
    
    /**
     * Generate a dynamic warmup routine
     */
    private fun generateWarmup(targetMuscles: List<MuscleGroup>): List<WorkoutExercise> {
        // This would ideally pull from exercise repository
        // Simplified for scaffold
        return emptyList()  // TODO: Implement warmup generation
    }
    
    /**
     * Generate a cooldown routine
     */
    private fun generateCooldown(): List<WorkoutExercise> {
        return emptyList()  // TODO: Implement cooldown generation
    }
    
    private fun generateWorkoutName(muscles: List<MuscleGroup>, goal: FitnessGoal?): String {
        val muscleText = when {
            muscles.size == 1 -> muscles.first().name.lowercase().replaceFirstChar { it.uppercase() }
            muscles.containsAll(listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS)) -> "Push"
            muscles.containsAll(listOf(MuscleGroup.BACK, MuscleGroup.BICEPS)) -> "Pull"
            muscles.any { it in listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES) } 
                && muscles.none { it in listOf(MuscleGroup.CHEST, MuscleGroup.BACK) } -> "Legs"
            muscles.size > 3 -> "Full Body"
            else -> muscles.take(2).joinToString(" & ") { 
                it.name.lowercase().replaceFirstChar { c -> c.uppercase() } 
            }
        }
        
        val goalText = when (goal) {
            FitnessGoal.BUILD_STRENGTH -> "Strength"
            FitnessGoal.BUILD_MUSCLE -> "Hypertrophy"
            FitnessGoal.LOSE_WEIGHT -> "Burn"
            FitnessGoal.IMPROVE_ENDURANCE -> "Endurance"
            else -> ""
        }
        
        return listOfNotNull(muscleText, goalText.takeIf { it.isNotEmpty() }).joinToString(" ")
    }
    
    private fun generateDescription(muscles: List<MuscleGroup>, goal: FitnessGoal?): String {
        val goalDescription = when (goal) {
            FitnessGoal.BUILD_STRENGTH -> "focused on building raw strength with heavy compound movements"
            FitnessGoal.BUILD_MUSCLE -> "designed for muscle growth with moderate weight and volume"
            FitnessGoal.LOSE_WEIGHT -> "optimized for calorie burn with minimal rest periods"
            FitnessGoal.IMPROVE_ENDURANCE -> "built to improve cardiovascular endurance"
            FitnessGoal.INCREASE_FLEXIBILITY -> "centered on mobility and flexibility"
            FitnessGoal.ATHLETIC_PERFORMANCE -> "designed for explosive power and athleticism"
            else -> "balanced for overall fitness"
        }
        return "A workout $goalDescription, targeting ${muscles.size} muscle groups."
    }
}

class NoExercisesAvailableException(message: String) : Exception(message)
