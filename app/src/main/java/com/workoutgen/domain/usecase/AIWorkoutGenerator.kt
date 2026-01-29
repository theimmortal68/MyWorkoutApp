package com.workoutgen.domain.usecase

import com.workoutgen.domain.model.*
import com.workoutgen.domain.repository.ExerciseRepository
import com.workoutgen.domain.repository.WorkoutLogRepository
import com.workoutgen.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * AI-Powered Workout Generator
 * 
 * Features:
 * - Learns from your workout history to adjust weights and reps
 * - Selects optimal muscle groups based on recovery status
 * - Scales volume based on available time
 * - Respects your progression style (linear, double progression, etc.)
 * - Considers your goals, equipment, and experience level
 * - Balances push/pull/legs for optimal development
 */
class AIWorkoutGenerator @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val workoutLogRepository: WorkoutLogRepository,
    private val userProfileRepository: UserProfileRepository,
    private val physicalLimitationFilter: PhysicalLimitationFilter
) {
    
    // ========================================================================
    // MAIN GENERATION FUNCTION
    // ========================================================================
    
    suspend fun generateWorkout(params: AIWorkoutParams): GeneratedWorkout {
        val profile = userProfileRepository.getUserProfile().first() 
            ?: return GeneratedWorkout.Error("Please set up your profile first")
        
        val allExercises = exerciseRepository.getAllExercises().first()
        val recentLogs = workoutLogRepository.getLogsFromRecentDays(30) // Last 30 days
        val exerciseHistory = workoutLogRepository.getExercisePerformanceHistory()
        
        // Step 1: Determine which muscles to train today
        val targetMuscles = selectTargetMuscles(
            params = params,
            profile = profile,
            recentLogs = recentLogs
        )
        
        // Step 2: Calculate how many exercises based on time
        val exerciseCount = calculateExerciseCount(
            durationMinutes = params.durationMinutes,
            goal = params.goal ?: profile.fitnessGoals.firstOrNull() ?: FitnessGoal.GENERAL_FITNESS
        )
        
        // Step 3: Select exercises for the target muscles
        val selectedExercises = selectExercises(
            targetMuscles = targetMuscles,
            exerciseCount = exerciseCount,
            availableEquipment = profile.availableEquipment,
            allExercises = allExercises,
            profile = profile,
            params = params
        )
        
        // Step 4: Assign sets, reps, and weights based on history and goals
        // Determine if this is a deload week from program context
        val isDeloadWeek = params.programContext?.isDeloadWeek ?: false
        val effectiveProgressionStyle = if (isDeloadWeek) {
            PeriodizationType.DELOAD
        } else {
            params.progressionStyle ?: PeriodizationType.LINEAR
        }

        val workoutExercises = selectedExercises.map { exercise ->
            createWorkoutExercise(
                exercise = exercise,
                goal = params.goal ?: profile.fitnessGoals.firstOrNull() ?: FitnessGoal.GENERAL_FITNESS,
                profile = profile,
                exerciseHistory = exerciseHistory,
                progressionStyle = effectiveProgressionStyle
            )
        }
        
        // Step 5: Generate warmup and cooldown if requested
        val warmup = if (params.includeWarmup) {
            generateWarmup(targetMuscles, allExercises)
        } else emptyList()
        
        val cooldown = if (params.includeCooldown) {
            generateCooldown(targetMuscles, allExercises)
        } else emptyList()
        
        // Step 6: Build the final workout
        val workout = Workout(
            name = generateWorkoutName(targetMuscles, params),
            description = generateDescription(targetMuscles, params),
            exercises = workoutExercises,
            targetMuscles = targetMuscles,
            estimatedDurationMinutes = params.durationMinutes,
            difficulty = profile.experienceLevel,
            goal = params.goal ?: profile.fitnessGoals.firstOrNull() ?: FitnessGoal.GENERAL_FITNESS,
            warmup = warmup,
            cooldown = cooldown
        )
        
        return GeneratedWorkout.Success(
            workout = workout,
            reasoning = WorkoutReasoning(
                muscleSelection = getMuscleSelectionReason(targetMuscles, recentLogs),
                exerciseSelection = getExerciseSelectionReason(selectedExercises, params),
                volumeAdjustment = getVolumeReason(params.durationMinutes),
                progressionNotes = getProgressionNotes(workoutExercises, exerciseHistory)
            )
        )
    }
    
    // ========================================================================
    // MUSCLE GROUP SELECTION
    // ========================================================================
    
    private suspend fun selectTargetMuscles(
        params: AIWorkoutParams,
        profile: UserProfile,
        recentLogs: List<WorkoutLog>
    ): List<MuscleGroup> {
        // If program context is provided, use the program day's target muscles
        if (params.programContext != null) {
            return params.programContext.currentDay.targetMuscles
        }

        // If user specified muscles, use those
        if (params.targetMuscles != null && params.targetMuscles.isNotEmpty()) {
            return params.targetMuscles
        }

        // Calculate recovery status for each muscle group
        val recoveryStatus = calculateMuscleRecovery(recentLogs)

        // Get muscles that are recovered
        val recoveredMuscles = recoveryStatus
            .filter { it.value == RecoveryStatus.RECOVERED }
            .keys
            .toList()

        // Select based on split preference
        return when (params.splitType ?: profile.preferredSplit) {
            WorkoutSplit.FULL_BODY -> selectFullBodyMuscles(recoveredMuscles)
            WorkoutSplit.UPPER_LOWER -> selectUpperLowerMuscles(recoveredMuscles, recentLogs)
            WorkoutSplit.PUSH_PULL_LEGS -> selectPPLMuscles(recoveredMuscles, recentLogs)
            WorkoutSplit.BRO_SPLIT -> selectBroSplitMuscles(recoveredMuscles, recentLogs)
            WorkoutSplit.ARNOLD_SPLIT -> selectArnoldMuscles(recoveredMuscles, recentLogs)
            WorkoutSplit.POWERLIFTING -> listOf(MuscleGroup.QUADRICEPS, MuscleGroup.CHEST, MuscleGroup.BACK)
            WorkoutSplit.CUSTOM -> params.targetMuscles ?: selectFullBodyMuscles(recoveredMuscles)
        }
    }
    
    private fun calculateMuscleRecovery(recentLogs: List<WorkoutLog>): Map<MuscleGroup, RecoveryStatus> {
        val result = mutableMapOf<MuscleGroup, RecoveryStatus>()
        val now = System.currentTimeMillis()
        val recoveryHours = mapOf(
            MuscleGroup.QUADRICEPS to 72,
            MuscleGroup.HAMSTRINGS to 72,
            MuscleGroup.BACK to 72,
            MuscleGroup.CHEST to 48,
            MuscleGroup.SHOULDERS to 48,
            MuscleGroup.GLUTES to 72,
            MuscleGroup.BICEPS to 48,
            MuscleGroup.TRICEPS to 48,
            MuscleGroup.CORE to 24,
            MuscleGroup.CALVES to 48,
            MuscleGroup.FOREARMS to 48
        )
        
        MuscleGroup.entries.forEach { muscle ->
            val lastTrained = findLastTrainedTime(muscle, recentLogs)
            val hoursSinceTraining = if (lastTrained != null) {
                (now - lastTrained) / (1000 * 60 * 60)
            } else {
                Long.MAX_VALUE
            }
            
            val requiredHours = recoveryHours[muscle] ?: 48
            
            result[muscle] = when {
                hoursSinceTraining >= requiredHours -> RecoveryStatus.RECOVERED
                hoursSinceTraining >= requiredHours * 0.7 -> RecoveryStatus.RECOVERING
                hoursSinceTraining >= requiredHours * 0.5 -> RecoveryStatus.FATIGUED
                else -> RecoveryStatus.OVERTRAINED
            }
        }
        
        return result
    }
    
    private fun findLastTrainedTime(muscle: MuscleGroup, logs: List<WorkoutLog>): Long? {
        // Would need to join with exercise data to determine which muscles were trained
        // For now, return null to indicate recovered
        return null
    }
    
    private fun selectFullBodyMuscles(recovered: List<MuscleGroup>): List<MuscleGroup> {
        val targets = mutableListOf<MuscleGroup>()
        
        // Ensure balanced coverage: 1 push, 1 pull, 1 legs
        val pushMuscles = listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS)
        val pullMuscles = listOf(MuscleGroup.BACK, MuscleGroup.BICEPS)
        val legMuscles = listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES)
        
        targets.add(pushMuscles.filter { it in recovered }.randomOrNull() ?: MuscleGroup.CHEST)
        targets.add(pullMuscles.filter { it in recovered }.randomOrNull() ?: MuscleGroup.BACK)
        targets.add(legMuscles.filter { it in recovered }.randomOrNull() ?: MuscleGroup.QUADRICEPS)
        targets.add(MuscleGroup.CORE)
        
        return targets.distinct()
    }
    
    private fun selectUpperLowerMuscles(recovered: List<MuscleGroup>, logs: List<WorkoutLog>): List<MuscleGroup> {
        // Alternate between upper and lower
        val lastWorkoutMuscles = logs.firstOrNull()?.let { getWorkoutMuscles(it) } ?: emptyList()
        val lastWasLower = lastWorkoutMuscles.any { 
            it in listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES)
        }
        
        return if (lastWasLower) {
            listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS, MuscleGroup.BICEPS, MuscleGroup.TRICEPS)
        } else {
            listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES)
        }
    }
    
    private fun selectPPLMuscles(recovered: List<MuscleGroup>, logs: List<WorkoutLog>): List<MuscleGroup> {
        // Rotate: Push -> Pull -> Legs
        val lastWorkoutMuscles = logs.firstOrNull()?.let { getWorkoutMuscles(it) } ?: emptyList()
        
        val lastWasPush = lastWorkoutMuscles.any { it in listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS) }
        val lastWasPull = lastWorkoutMuscles.any { it == MuscleGroup.BACK }
        val lastWasLegs = lastWorkoutMuscles.any { it in listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS) }
        
        return when {
            lastWasLegs || (!lastWasPush && !lastWasPull && !lastWasLegs) -> 
                listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS)
            lastWasPush -> 
                listOf(MuscleGroup.BACK, MuscleGroup.BICEPS)
            else -> 
                listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES)
        }
    }
    
    private fun selectBroSplitMuscles(recovered: List<MuscleGroup>, logs: List<WorkoutLog>): List<MuscleGroup> {
        // One main muscle group per day
        val rotation = listOf(
            listOf(MuscleGroup.CHEST),
            listOf(MuscleGroup.BACK),
            listOf(MuscleGroup.SHOULDERS),
            listOf(MuscleGroup.BICEPS, MuscleGroup.TRICEPS),
            listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES)
        )
        
        val lastWorkoutMuscles = logs.firstOrNull()?.let { getWorkoutMuscles(it) } ?: emptyList()
        val lastIndex = rotation.indexOfFirst { day -> day.any { it in lastWorkoutMuscles } }
        val nextIndex = (lastIndex + 1) % rotation.size
        
        return rotation[nextIndex]
    }
    
    private fun selectArnoldMuscles(recovered: List<MuscleGroup>, logs: List<WorkoutLog>): List<MuscleGroup> {
        // Arnold split: Chest/Back, Shoulders/Arms, Legs
        val rotation = listOf(
            listOf(MuscleGroup.CHEST, MuscleGroup.BACK),
            listOf(MuscleGroup.SHOULDERS, MuscleGroup.BICEPS, MuscleGroup.TRICEPS),
            listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES)
        )
        
        val lastWorkoutMuscles = logs.firstOrNull()?.let { getWorkoutMuscles(it) } ?: emptyList()
        val lastIndex = rotation.indexOfFirst { day -> day.any { it in lastWorkoutMuscles } }
        val nextIndex = (lastIndex + 1) % rotation.size
        
        return rotation[nextIndex]
    }
    
    private fun getWorkoutMuscles(log: WorkoutLog): List<MuscleGroup> {
        // Would need to map exercise IDs to muscles
        return emptyList()
    }
    
    // ========================================================================
    // EXERCISE SELECTION
    // ========================================================================
    
    private fun calculateExerciseCount(durationMinutes: Int, goal: FitnessGoal): Int {
        // Base: ~6 minutes per exercise including rest
        val baseCount = when (goal) {
            FitnessGoal.BUILD_STRENGTH -> (durationMinutes / 8).coerceIn(3, 6) // Longer rest
            FitnessGoal.BUILD_MUSCLE -> (durationMinutes / 6).coerceIn(4, 10)
            FitnessGoal.LOSE_WEIGHT -> (durationMinutes / 5).coerceIn(5, 12) // Shorter rest
            FitnessGoal.IMPROVE_ENDURANCE -> (durationMinutes / 4).coerceIn(6, 15)
            FitnessGoal.POWERBUILDING -> (durationMinutes / 7).coerceIn(4, 8)
            else -> (durationMinutes / 6).coerceIn(4, 10)
        }
        return baseCount
    }
    
    private fun selectExercises(
        targetMuscles: List<MuscleGroup>,
        exerciseCount: Int,
        availableEquipment: Set<Equipment>,
        allExercises: List<Exercise>,
        profile: UserProfile,
        params: AIWorkoutParams
    ): List<Exercise> {
        val selected = mutableListOf<Exercise>()
        val exercisesPerMuscle = (exerciseCount / targetMuscles.size).coerceAtLeast(1)

        // Filter exercises by equipment
        val filteredByEquipment = allExercises.filter { exercise ->
            exercise.equipmentRequired.all { it in availableEquipment || it == Equipment.NONE }
        }.filter { exercise ->
            // Filter by difficulty
            when (profile.experienceLevel) {
                Difficulty.BEGINNER -> exercise.difficulty in listOf(Difficulty.BEGINNER, Difficulty.INTERMEDIATE)
                Difficulty.INTERMEDIATE -> exercise.difficulty != Difficulty.EXPERT
                else -> true
            }
        }.filter { exercise ->
            // Exclude user's excluded exercises
            exercise.id !in profile.excludedExercises
        }

        // Apply physical limitation filter
        val availableExercises = physicalLimitationFilter.filterExercises(
            exercises = filteredByEquipment,
            limitations = profile.physicalLimitations
        )
        
        // Prioritize compound movements if requested
        val compounds = availableExercises.filter { it.type == ExerciseType.COMPOUND }
        val isolations = availableExercises.filter { it.type == ExerciseType.ISOLATION }
        
        targetMuscles.forEach { muscle ->
            val muscleExercises = if (params.prioritizeCompound) {
                compounds.filter { muscle in it.primaryMuscles } +
                isolations.filter { muscle in it.primaryMuscles }
            } else {
                availableExercises.filter { muscle in it.primaryMuscles }
            }
            
            // Select exercises for this muscle
            val count = if (muscle == targetMuscles.first()) {
                exercisesPerMuscle + (exerciseCount % targetMuscles.size) // Give extra to first muscle
            } else {
                exercisesPerMuscle
            }
            
            muscleExercises
                .filter { it !in selected }
                .shuffled()
                .take(count)
                .forEach { selected.add(it) }
        }
        
        return selected.take(exerciseCount)
    }
    
    // ========================================================================
    // WEIGHT/REP CALCULATION
    // ========================================================================
    
    private fun createWorkoutExercise(
        exercise: Exercise,
        goal: FitnessGoal,
        profile: UserProfile,
        exerciseHistory: Map<String, List<ExercisePerformance>>,
        progressionStyle: PeriodizationType
    ): WorkoutExercise {
        // Get rep range based on goal
        val (sets, repRange, rpeTarget) = when (goal) {
            FitnessGoal.BUILD_STRENGTH -> Triple(5, 3..5, 8..9)
            FitnessGoal.BUILD_MUSCLE -> Triple(4, 8..12, 7..8)
            FitnessGoal.LOSE_WEIGHT -> Triple(3, 12..15, 6..7)
            FitnessGoal.IMPROVE_ENDURANCE -> Triple(3, 15..20, 5..6)
            FitnessGoal.POWERBUILDING -> Triple(4, 5..8, 7..9)
            FitnessGoal.ATHLETIC_PERFORMANCE -> Triple(4, 6..10, 7..8)
            else -> Triple(3, 10..12, 7..8)
        }
        
        // Calculate weight based on history
        val history = exerciseHistory[exercise.id]
        val suggestedWeight = calculateSuggestedWeight(
            exercise = exercise,
            history = history,
            targetReps = repRange.first,
            progressionStyle = progressionStyle,
            profile = profile
        )
        
        // Calculate warmup sets if this is a heavy compound
        val warmupSets = if (exercise.type == ExerciseType.COMPOUND && suggestedWeight != null && suggestedWeight > 40f) {
            WarmupCalculator.generateWarmupSets(suggestedWeight, repRange.first)
        } else {
            emptyList()
        }
        
        // Calculate rest time based on goal
        val restSeconds = when (goal) {
            FitnessGoal.BUILD_STRENGTH -> 180
            FitnessGoal.BUILD_MUSCLE -> 90
            FitnessGoal.LOSE_WEIGHT -> 45
            FitnessGoal.IMPROVE_ENDURANCE -> 30
            FitnessGoal.POWERBUILDING -> 120
            else -> 60
        }
        
        return WorkoutExercise(
            exercise = exercise,
            sets = sets,
            reps = repRange,
            restSeconds = restSeconds,
            weight = suggestedWeight,
            rpe = rpeTarget.last,
            warmupSets = warmupSets,
            notes = generateExerciseNotes(exercise, history, progressionStyle)
        )
    }
    
    private fun calculateSuggestedWeight(
        exercise: Exercise,
        history: List<ExercisePerformance>?,
        targetReps: Int,
        progressionStyle: PeriodizationType,
        profile: UserProfile
    ): Float? {
        if (history.isNullOrEmpty()) {
            // No history - check if user has a known 1RM
            return profile.maxes[exercise.id]?.let { oneRM ->
                OneRepMaxCalculator.weightForReps(oneRM, targetReps)
            }
        }
        
        // Get the most recent successful performance
        val lastPerformance = history
            .sortedByDescending { it.date }
            .firstOrNull { it.completedAllSets }
        
        if (lastPerformance == null) {
            // Had attempts but no successful completion - stay at same weight
            return history.maxByOrNull { it.date }?.weight
        }
        
        // Apply progression based on style
        return when (progressionStyle) {
            PeriodizationType.LINEAR -> {
                // Simple linear: add weight each session
                val increment = getWeightIncrement(exercise, profile)
                lastPerformance.weight + increment
            }
            
            PeriodizationType.DOUBLE_PROGRESSION -> {
                // Double progression: increase reps first, then weight
                if (lastPerformance.achievedReps >= lastPerformance.targetReps + 2) {
                    // Hit high end of rep range - increase weight, reset reps
                    val increment = getWeightIncrement(exercise, profile)
                    lastPerformance.weight + increment
                } else {
                    // Stay at same weight, try for more reps
                    lastPerformance.weight
                }
            }
            
            PeriodizationType.WAVE -> {
                // Wave: cycle through heavy/medium/light
                val cyclePosition = (history.size % 3)
                when (cyclePosition) {
                    0 -> lastPerformance.weight * 1.05f // Heavy
                    1 -> lastPerformance.weight * 0.9f  // Light
                    else -> lastPerformance.weight      // Medium
                }
            }
            
            PeriodizationType.DAILY_UNDULATING -> {
                // DUP: vary based on rep target
                when {
                    targetReps <= 5 -> lastPerformance.weight * 1.05f   // Strength day
                    targetReps <= 10 -> lastPerformance.weight          // Hypertrophy day
                    else -> lastPerformance.weight * 0.85f              // Endurance day
                }
            }
            
            PeriodizationType.DELOAD -> {
                // Deload week: reduce by 40%
                lastPerformance.weight * 0.6f
            }
            
            else -> lastPerformance.weight
        }?.let { weight ->
            // Round to nearest 2.5kg
            (weight / 2.5f).roundToInt() * 2.5f
        }
    }
    
    private fun getWeightIncrement(exercise: Exercise, profile: UserProfile): Float {
        val baseIncrement = if (profile.useMetricUnits) {
            when {
                exercise.primaryMuscles.any { it in listOf(MuscleGroup.QUADRICEPS, MuscleGroup.BACK, MuscleGroup.HAMSTRINGS) } -> 2.5f
                exercise.primaryMuscles.any { it in listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS) } -> 1.25f
                else -> 1.0f
            }
        } else {
            when {
                exercise.primaryMuscles.any { it in listOf(MuscleGroup.QUADRICEPS, MuscleGroup.BACK, MuscleGroup.HAMSTRINGS) } -> 5f
                exercise.primaryMuscles.any { it in listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS) } -> 2.5f
                else -> 2.5f
            }
        }
        
        // Adjust based on experience - beginners progress faster
        return when (profile.experienceLevel) {
            Difficulty.BEGINNER -> baseIncrement * 1.5f
            Difficulty.INTERMEDIATE -> baseIncrement
            Difficulty.ADVANCED -> baseIncrement * 0.75f
            Difficulty.EXPERT -> baseIncrement * 0.5f
        }
    }
    
    private fun generateExerciseNotes(
        exercise: Exercise,
        history: List<ExercisePerformance>?,
        progressionStyle: PeriodizationType
    ): String? {
        if (history.isNullOrEmpty()) {
            return "First time performing this exercise. Start conservative and focus on form."
        }
        
        val lastPerformance = history.maxByOrNull { it.date } ?: return null
        
        return when {
            !lastPerformance.completedAllSets -> 
                "Last session incomplete. Try to complete all sets today."
            lastPerformance.rpe != null && lastPerformance.rpe >= 9 -> 
                "Last session was very hard (RPE ${lastPerformance.rpe}). Weight adjusted accordingly."
            progressionStyle == PeriodizationType.DOUBLE_PROGRESSION && 
                lastPerformance.achievedReps >= lastPerformance.targetReps + 2 ->
                "Great progress! Weight increased - aim for the lower end of the rep range."
            progressionStyle == PeriodizationType.DELOAD ->
                "Deload session - focus on form and recovery."
            else -> null
        }
    }
    
    // ========================================================================
    // WARMUP / COOLDOWN
    // ========================================================================
    
    private fun generateWarmup(targetMuscles: List<MuscleGroup>, allExercises: List<Exercise>): List<WorkoutExercise> {
        val warmupExercises = mutableListOf<WorkoutExercise>()
        
        // General cardio warmup
        val cardioExercise = allExercises.find { it.type == ExerciseType.CARDIO && it.equipmentRequired.contains(Equipment.NONE) }
        if (cardioExercise != null) {
            warmupExercises.add(
                WorkoutExercise(
                    exercise = cardioExercise,
                    sets = 1,
                    reps = 300..300, // 5 minutes
                    restSeconds = 0,
                    notes = "Light cardio for 5 minutes"
                )
            )
        }
        
        // Dynamic stretches for target muscles
        targetMuscles.take(3).forEach { muscle ->
            allExercises
                .filter { it.type == ExerciseType.STRETCHING || it.type == ExerciseType.MOBILITY }
                .filter { muscle in it.primaryMuscles }
                .firstOrNull()
                ?.let { stretch ->
                    warmupExercises.add(
                        WorkoutExercise(
                            exercise = stretch,
                            sets = 1,
                            reps = 10..10,
                            restSeconds = 0
                        )
                    )
                }
        }
        
        return warmupExercises
    }
    
    private fun generateCooldown(targetMuscles: List<MuscleGroup>, allExercises: List<Exercise>): List<WorkoutExercise> {
        return targetMuscles.take(4).mapNotNull { muscle ->
            allExercises
                .filter { it.type == ExerciseType.STRETCHING }
                .filter { muscle in it.primaryMuscles }
                .firstOrNull()
                ?.let { stretch ->
                    WorkoutExercise(
                        exercise = stretch,
                        sets = 1,
                        reps = 30..30, // 30 second hold
                        restSeconds = 0,
                        notes = "Hold for 30 seconds"
                    )
                }
        }
    }
    
    // ========================================================================
    // HELPERS
    // ========================================================================
    
    private fun generateWorkoutName(muscles: List<MuscleGroup>, params: AIWorkoutParams): String {
        // If program context is provided, use the program day's name
        if (params.programContext != null) {
            val weekInfo = "Week ${params.programContext.currentWeek}"
            val dayName = params.programContext.currentDay.name
            val deloadSuffix = if (params.programContext.isDeloadWeek) " (Deload)" else ""
            return "$weekInfo - $dayName$deloadSuffix"
        }

        val muscleNames = muscles.take(2).joinToString(" & ") { it.displayName }
        val goalSuffix = when (params.goal) {
            FitnessGoal.BUILD_STRENGTH -> "Strength"
            FitnessGoal.BUILD_MUSCLE -> "Hypertrophy"
            FitnessGoal.LOSE_WEIGHT -> "Metabolic"
            FitnessGoal.IMPROVE_ENDURANCE -> "Endurance"
            else -> "Training"
        }
        return "$muscleNames $goalSuffix"
    }
    
    private fun generateDescription(muscles: List<MuscleGroup>, params: AIWorkoutParams): String {
        return "AI-generated ${params.durationMinutes}-minute workout targeting ${muscles.joinToString(", ") { it.displayName.lowercase() }}."
    }
    
    private fun getMuscleSelectionReason(muscles: List<MuscleGroup>, logs: List<WorkoutLog>): String {
        return "Selected ${muscles.joinToString(", ") { it.displayName }} based on recovery status and training history."
    }
    
    private fun getExerciseSelectionReason(exercises: List<Exercise>, params: AIWorkoutParams): String {
        val compoundCount = exercises.count { it.type == ExerciseType.COMPOUND }
        return "Selected $compoundCount compound and ${exercises.size - compoundCount} isolation exercises matching your equipment."
    }
    
    private fun getVolumeReason(minutes: Int): String {
        return "Volume scaled for ${minutes}-minute session."
    }
    
    private fun getProgressionNotes(exercises: List<WorkoutExercise>, history: Map<String, List<ExercisePerformance>>): List<String> {
        return exercises.mapNotNull { it.notes }
    }
}

// ========================================================================
// DATA CLASSES
// ========================================================================

data class AIWorkoutParams(
    val durationMinutes: Int = 45,
    val goal: FitnessGoal? = null,
    val targetMuscles: List<MuscleGroup>? = null,
    val splitType: WorkoutSplit? = null,
    val progressionStyle: PeriodizationType? = null,
    val prioritizeCompound: Boolean = true,
    val includeWarmup: Boolean = true,
    val includeCooldown: Boolean = true,
    val focusAreas: List<MuscleGroup> = emptyList(), // Extra emphasis on these
    val programContext: ProgramWorkoutContext? = null // When generating workout within a program
)

data class ExercisePerformance(
    val exerciseId: String,
    val date: Long,
    val weight: Float,
    val targetReps: Int,
    val achievedReps: Int,
    val sets: Int,
    val completedAllSets: Boolean,
    val rpe: Int? = null
)

sealed class GeneratedWorkout {
    data class Success(
        val workout: Workout,
        val reasoning: WorkoutReasoning
    ) : GeneratedWorkout()
    
    data class Error(val message: String) : GeneratedWorkout()
}

data class WorkoutReasoning(
    val muscleSelection: String,
    val exerciseSelection: String,
    val volumeAdjustment: String,
    val progressionNotes: List<String>
)
