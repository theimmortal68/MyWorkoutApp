package com.workoutgen.domain.usecase

import com.workoutgen.domain.model.*
import com.workoutgen.domain.repository.ExerciseRepository
import com.workoutgen.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

/**
 * AI-Powered Program Generator
 *
 * Creates personalized multi-week training program frameworks based on:
 * - Days per week availability
 * - Fitness goals
 * - User experience level
 * - Available equipment
 * - Physical limitations
 *
 * Unlike AIWorkoutGenerator (which creates one-off workouts), this generates
 * a complete program structure that guides how individual workouts are generated.
 */
class AIProgramGenerator @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val userProfileRepository: UserProfileRepository,
    private val physicalLimitationFilter: PhysicalLimitationFilter
) {

    // ========================================================================
    // MAIN GENERATION FUNCTION
    // ========================================================================

    suspend fun generateProgram(params: AIProgramGeneratorParams): GeneratedProgram {
        val profile = userProfileRepository.getUserProfile().first()
            ?: return GeneratedProgram.Error("Please set up your profile first")

        val allExercises = exerciseRepository.getAllExercises().first()

        // Step 1: Select optimal split based on days/week and goal
        val selectedSplit = params.preferredSplit
            ?: selectOptimalSplit(params.daysPerWeek, params.primaryGoal, profile.experienceLevel)

        // Step 2: Select periodization based on goal and experience
        val periodization = selectPeriodization(params.primaryGoal, profile.experienceLevel)

        // Step 3: Generate program days
        val programDays = generateProgramDays(
            split = selectedSplit,
            daysPerWeek = params.daysPerWeek,
            goal = params.primaryGoal,
            emphasisMuscles = params.emphasisMuscles,
            profile = profile,
            allExercises = allExercises
        )

        // Step 4: Determine required equipment based on selected exercises
        val requiredEquipment = programDays
            .flatMap { day -> day.exercises }
            .flatMap { exercise ->
                allExercises.find { it.id == exercise.exerciseId }?.equipmentRequired ?: emptyList()
            }
            .toSet()

        // Step 5: Build the program
        val program = TrainingProgram(
            id = UUID.randomUUID().toString(),
            name = params.programName ?: generateProgramName(params.primaryGoal, selectedSplit),
            description = generateProgramDescription(params, selectedSplit, periodization),
            author = "AI Generated",
            difficulty = profile.experienceLevel,
            goals = listOf(params.primaryGoal),
            durationWeeks = params.durationWeeks,
            daysPerWeek = params.daysPerWeek,
            split = selectedSplit,
            periodization = periodization,
            requiredEquipment = requiredEquipment,
            deloadFrequency = if (params.includeDeloads) params.deloadFrequency else 0,
            programDays = programDays,
            progressionRules = generateProgressionRules(params.primaryGoal, periodization)
        )

        // Step 6: Build reasoning explanation
        val reasoning = ProgramReasoning(
            splitSelection = getSplitSelectionReason(selectedSplit, params.daysPerWeek, params.primaryGoal),
            periodizationSelection = getPeriodizationReason(periodization, params.primaryGoal, profile.experienceLevel),
            volumeDistribution = getVolumeDistributionReason(programDays, params.primaryGoal),
            progressionStrategy = getProgressionStrategyReason(periodization, params.primaryGoal),
            limitationAdjustments = getLimitationAdjustments(profile.physicalLimitations)
        )

        return GeneratedProgram.Success(
            AIGeneratedProgram(
                program = program,
                reasoning = reasoning
            )
        )
    }

    // ========================================================================
    // SPLIT SELECTION
    // ========================================================================

    /**
     * Select optimal workout split based on training frequency and goals
     */
    fun selectOptimalSplit(
        daysPerWeek: Int,
        goal: FitnessGoal,
        experience: Difficulty
    ): WorkoutSplit {
        return when (daysPerWeek) {
            2 -> WorkoutSplit.FULL_BODY
            3 -> when (goal) {
                FitnessGoal.BUILD_STRENGTH -> WorkoutSplit.FULL_BODY
                FitnessGoal.POWERBUILDING -> WorkoutSplit.FULL_BODY
                else -> WorkoutSplit.PUSH_PULL_LEGS
            }
            4 -> WorkoutSplit.UPPER_LOWER
            5 -> when (goal) {
                FitnessGoal.BUILD_MUSCLE -> WorkoutSplit.PUSH_PULL_LEGS
                else -> WorkoutSplit.UPPER_LOWER
            }
            6 -> when {
                experience == Difficulty.ADVANCED || experience == Difficulty.EXPERT ->
                    WorkoutSplit.ARNOLD_SPLIT
                else -> WorkoutSplit.PUSH_PULL_LEGS
            }
            else -> WorkoutSplit.FULL_BODY
        }
    }

    // ========================================================================
    // PERIODIZATION SELECTION
    // ========================================================================

    private fun selectPeriodization(goal: FitnessGoal, experience: Difficulty): PeriodizationType {
        return when {
            experience == Difficulty.BEGINNER && goal == FitnessGoal.BUILD_STRENGTH ->
                PeriodizationType.LINEAR
            experience == Difficulty.BEGINNER && goal == FitnessGoal.BUILD_MUSCLE ->
                PeriodizationType.DOUBLE_PROGRESSION
            experience == Difficulty.BEGINNER ->
                PeriodizationType.LINEAR
            experience == Difficulty.INTERMEDIATE && goal == FitnessGoal.BUILD_STRENGTH ->
                PeriodizationType.WAVE
            experience == Difficulty.INTERMEDIATE && goal == FitnessGoal.BUILD_MUSCLE ->
                PeriodizationType.DOUBLE_PROGRESSION
            experience == Difficulty.INTERMEDIATE ->
                PeriodizationType.DOUBLE_PROGRESSION
            experience == Difficulty.ADVANCED || experience == Difficulty.EXPERT ->
                when (goal) {
                    FitnessGoal.BUILD_STRENGTH -> PeriodizationType.BLOCK
                    FitnessGoal.BUILD_MUSCLE -> PeriodizationType.DAILY_UNDULATING
                    FitnessGoal.POWERBUILDING -> PeriodizationType.DAILY_UNDULATING
                    else -> PeriodizationType.WEEKLY_UNDULATING
                }
            else -> PeriodizationType.LINEAR
        }
    }

    // ========================================================================
    // PROGRAM DAY GENERATION
    // ========================================================================

    private suspend fun generateProgramDays(
        split: WorkoutSplit,
        daysPerWeek: Int,
        goal: FitnessGoal,
        emphasisMuscles: List<MuscleGroup>,
        profile: UserProfile,
        allExercises: List<Exercise>
    ): List<ProgramDay> {
        return when (split) {
            WorkoutSplit.FULL_BODY -> generateFullBodyDays(daysPerWeek, goal, emphasisMuscles, profile, allExercises)
            WorkoutSplit.UPPER_LOWER -> generateUpperLowerDays(daysPerWeek, goal, emphasisMuscles, profile, allExercises)
            WorkoutSplit.PUSH_PULL_LEGS -> generatePPLDays(daysPerWeek, goal, emphasisMuscles, profile, allExercises)
            WorkoutSplit.ARNOLD_SPLIT -> generateArnoldDays(goal, emphasisMuscles, profile, allExercises)
            WorkoutSplit.BRO_SPLIT -> generateBroSplitDays(daysPerWeek, goal, emphasisMuscles, profile, allExercises)
            WorkoutSplit.POWERLIFTING -> generatePowerliftingDays(daysPerWeek, goal, profile, allExercises)
            WorkoutSplit.CUSTOM -> generateFullBodyDays(daysPerWeek, goal, emphasisMuscles, profile, allExercises)
        }
    }

    private suspend fun generateFullBodyDays(
        daysPerWeek: Int,
        goal: FitnessGoal,
        emphasisMuscles: List<MuscleGroup>,
        profile: UserProfile,
        allExercises: List<Exercise>
    ): List<ProgramDay> {
        val days = mutableListOf<ProgramDay>()
        val rotations = listOf(
            listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.QUADRICEPS, MuscleGroup.CORE),
            listOf(MuscleGroup.SHOULDERS, MuscleGroup.BACK, MuscleGroup.HAMSTRINGS, MuscleGroup.CORE),
            listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.GLUTES, MuscleGroup.BICEPS, MuscleGroup.TRICEPS)
        )

        for (dayNum in 1..daysPerWeek) {
            val targetMuscles = rotations[(dayNum - 1) % rotations.size].let { muscles ->
                if (emphasisMuscles.isNotEmpty()) {
                    (emphasisMuscles + muscles).distinct()
                } else muscles
            }

            val dayType = when {
                goal == FitnessGoal.BUILD_STRENGTH -> "Strength"
                goal == FitnessGoal.BUILD_MUSCLE && dayNum % 2 == 1 -> "Strength"
                goal == FitnessGoal.BUILD_MUSCLE -> "Volume"
                else -> "Balanced"
            }

            days.add(
                ProgramDay(
                    dayNumber = dayNum,
                    name = "Full Body $dayType ${('A'.code + (dayNum - 1)).toChar()}",
                    targetMuscles = targetMuscles,
                    exercises = selectExercisesForDay(
                        targetMuscles = targetMuscles,
                        exerciseCount = if (goal == FitnessGoal.BUILD_STRENGTH) 4 else 5,
                        goal = goal,
                        isStrengthFocused = dayType == "Strength",
                        profile = profile,
                        allExercises = allExercises
                    )
                )
            )
        }

        return days
    }

    private suspend fun generateUpperLowerDays(
        daysPerWeek: Int,
        goal: FitnessGoal,
        emphasisMuscles: List<MuscleGroup>,
        profile: UserProfile,
        allExercises: List<Exercise>
    ): List<ProgramDay> {
        val days = mutableListOf<ProgramDay>()
        val upperMuscles = listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS, MuscleGroup.BICEPS, MuscleGroup.TRICEPS)
        val lowerMuscles = listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES)

        val schedule = when (daysPerWeek) {
            3 -> listOf("Upper A", "Lower A", "Upper B")
            4 -> listOf("Upper A (Strength)", "Lower A (Strength)", "Upper B (Volume)", "Lower B (Volume)")
            5 -> listOf("Upper A (Strength)", "Lower A (Strength)", "Upper B (Volume)", "Lower B (Volume)", "Upper C (Pump)")
            else -> listOf("Upper A", "Lower A")
        }

        schedule.forEachIndexed { index, dayName ->
            val isUpper = dayName.contains("Upper")
            val isStrengthFocused = dayName.contains("Strength") ||
                (goal == FitnessGoal.BUILD_STRENGTH && !dayName.contains("Volume"))

            val baseMuscles = if (isUpper) upperMuscles else lowerMuscles
            val targetMuscles = if (emphasisMuscles.isNotEmpty()) {
                val relevant = emphasisMuscles.filter { it in baseMuscles }
                if (relevant.isNotEmpty()) (relevant + baseMuscles).distinct() else baseMuscles
            } else baseMuscles

            days.add(
                ProgramDay(
                    dayNumber = index + 1,
                    name = dayName,
                    targetMuscles = targetMuscles,
                    exercises = selectExercisesForDay(
                        targetMuscles = targetMuscles,
                        exerciseCount = if (isStrengthFocused) 5 else 6,
                        goal = goal,
                        isStrengthFocused = isStrengthFocused,
                        profile = profile,
                        allExercises = allExercises
                    )
                )
            )
        }

        return days
    }

    private suspend fun generatePPLDays(
        daysPerWeek: Int,
        goal: FitnessGoal,
        emphasisMuscles: List<MuscleGroup>,
        profile: UserProfile,
        allExercises: List<Exercise>
    ): List<ProgramDay> {
        val days = mutableListOf<ProgramDay>()
        val pushMuscles = listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS)
        val pullMuscles = listOf(MuscleGroup.BACK, MuscleGroup.BICEPS)
        val legMuscles = listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES)

        val schedule = when (daysPerWeek) {
            3 -> listOf(
                "Push" to pushMuscles,
                "Pull" to pullMuscles,
                "Legs" to legMuscles
            )
            4 -> listOf(
                "Push A (Chest)" to listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
                "Pull A (Back)" to listOf(MuscleGroup.BACK, MuscleGroup.BICEPS),
                "Legs A (Quads)" to listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.CALVES),
                "Push B (Shoulders)" to listOf(MuscleGroup.SHOULDERS, MuscleGroup.CHEST, MuscleGroup.TRICEPS)
            )
            5 -> listOf(
                "Push A (Heavy)" to pushMuscles,
                "Pull A (Heavy)" to pullMuscles,
                "Legs A (Heavy)" to legMuscles,
                "Push B (Volume)" to pushMuscles,
                "Pull B (Volume)" to pullMuscles
            )
            6 -> listOf(
                "Push A (Chest Focus)" to pushMuscles,
                "Pull A (Back Focus)" to pullMuscles,
                "Legs A (Quad Focus)" to listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES),
                "Push B (Shoulder Focus)" to listOf(MuscleGroup.SHOULDERS, MuscleGroup.CHEST, MuscleGroup.TRICEPS),
                "Pull B (Width Focus)" to pullMuscles,
                "Legs B (Posterior Focus)" to listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.QUADRICEPS, MuscleGroup.CALVES)
            )
            else -> listOf(
                "Push" to pushMuscles,
                "Pull" to pullMuscles,
                "Legs" to legMuscles
            )
        }

        schedule.forEachIndexed { index, (dayName, muscles) ->
            val isStrengthFocused = dayName.contains("Heavy") ||
                (goal == FitnessGoal.BUILD_STRENGTH && !dayName.contains("Volume"))

            val targetMuscles = if (emphasisMuscles.isNotEmpty()) {
                val relevant = emphasisMuscles.filter { it in muscles }
                if (relevant.isNotEmpty()) (relevant + muscles).distinct() else muscles
            } else muscles

            days.add(
                ProgramDay(
                    dayNumber = index + 1,
                    name = dayName,
                    targetMuscles = targetMuscles,
                    exercises = selectExercisesForDay(
                        targetMuscles = targetMuscles,
                        exerciseCount = if (isStrengthFocused) 5 else 7,
                        goal = goal,
                        isStrengthFocused = isStrengthFocused,
                        profile = profile,
                        allExercises = allExercises
                    )
                )
            )
        }

        return days
    }

    private suspend fun generateArnoldDays(
        goal: FitnessGoal,
        emphasisMuscles: List<MuscleGroup>,
        profile: UserProfile,
        allExercises: List<Exercise>
    ): List<ProgramDay> {
        val schedule = listOf(
            "Chest & Back A" to listOf(MuscleGroup.CHEST, MuscleGroup.BACK),
            "Shoulders & Arms A" to listOf(MuscleGroup.SHOULDERS, MuscleGroup.BICEPS, MuscleGroup.TRICEPS),
            "Legs A" to listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES),
            "Chest & Back B" to listOf(MuscleGroup.CHEST, MuscleGroup.BACK),
            "Shoulders & Arms B" to listOf(MuscleGroup.SHOULDERS, MuscleGroup.BICEPS, MuscleGroup.TRICEPS),
            "Legs B" to listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES)
        )

        return schedule.mapIndexed { index, (dayName, muscles) ->
            val isADay = dayName.contains("A")
            ProgramDay(
                dayNumber = index + 1,
                name = dayName,
                targetMuscles = if (emphasisMuscles.isNotEmpty()) {
                    val relevant = emphasisMuscles.filter { it in muscles }
                    if (relevant.isNotEmpty()) (relevant + muscles).distinct() else muscles
                } else muscles,
                exercises = selectExercisesForDay(
                    targetMuscles = muscles,
                    exerciseCount = 6,
                    goal = goal,
                    isStrengthFocused = isADay && goal != FitnessGoal.BUILD_MUSCLE,
                    profile = profile,
                    allExercises = allExercises
                )
            )
        }
    }

    private suspend fun generateBroSplitDays(
        daysPerWeek: Int,
        goal: FitnessGoal,
        emphasisMuscles: List<MuscleGroup>,
        profile: UserProfile,
        allExercises: List<Exercise>
    ): List<ProgramDay> {
        val schedule = listOf(
            "Chest Day" to listOf(MuscleGroup.CHEST),
            "Back Day" to listOf(MuscleGroup.BACK),
            "Shoulders Day" to listOf(MuscleGroup.SHOULDERS),
            "Arms Day" to listOf(MuscleGroup.BICEPS, MuscleGroup.TRICEPS),
            "Legs Day" to listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES)
        ).take(daysPerWeek)

        return schedule.mapIndexed { index, (dayName, muscles) ->
            ProgramDay(
                dayNumber = index + 1,
                name = dayName,
                targetMuscles = if (emphasisMuscles.isNotEmpty() && emphasisMuscles.any { it in muscles }) {
                    (emphasisMuscles.filter { it in muscles } + muscles).distinct()
                } else muscles,
                exercises = selectExercisesForDay(
                    targetMuscles = muscles,
                    exerciseCount = if (muscles.size == 1) 5 else 6,
                    goal = goal,
                    isStrengthFocused = false,
                    profile = profile,
                    allExercises = allExercises
                )
            )
        }
    }

    private suspend fun generatePowerliftingDays(
        daysPerWeek: Int,
        goal: FitnessGoal,
        profile: UserProfile,
        allExercises: List<Exercise>
    ): List<ProgramDay> {
        val schedule = when (daysPerWeek) {
            3 -> listOf(
                "Squat Day" to listOf(MuscleGroup.QUADRICEPS, MuscleGroup.GLUTES, MuscleGroup.CORE),
                "Bench Day" to listOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
                "Deadlift Day" to listOf(MuscleGroup.BACK, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES)
            )
            4 -> listOf(
                "Squat Day" to listOf(MuscleGroup.QUADRICEPS, MuscleGroup.GLUTES, MuscleGroup.CORE),
                "Bench Day" to listOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
                "Deadlift Day" to listOf(MuscleGroup.BACK, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
                "Accessory Day" to listOf(MuscleGroup.BACK, MuscleGroup.SHOULDERS, MuscleGroup.BICEPS)
            )
            else -> listOf(
                "Squat Day" to listOf(MuscleGroup.QUADRICEPS, MuscleGroup.GLUTES),
                "Bench Day" to listOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS),
                "Deadlift Day" to listOf(MuscleGroup.BACK, MuscleGroup.HAMSTRINGS)
            )
        }

        return schedule.mapIndexed { index, (dayName, muscles) ->
            ProgramDay(
                dayNumber = index + 1,
                name = dayName,
                targetMuscles = muscles,
                exercises = selectExercisesForDay(
                    targetMuscles = muscles,
                    exerciseCount = 4,
                    goal = FitnessGoal.BUILD_STRENGTH,
                    isStrengthFocused = true,
                    profile = profile,
                    allExercises = allExercises
                )
            )
        }
    }

    // ========================================================================
    // EXERCISE SELECTION FOR PROGRAM DAYS
    // ========================================================================

    private fun selectExercisesForDay(
        targetMuscles: List<MuscleGroup>,
        exerciseCount: Int,
        goal: FitnessGoal,
        isStrengthFocused: Boolean,
        profile: UserProfile,
        allExercises: List<Exercise>
    ): List<ProgramExercise> {
        val availableExercises = allExercises
            .filter { exercise ->
                exercise.equipmentRequired.all { it in profile.availableEquipment || it == Equipment.NONE }
            }
            .filter { exercise ->
                when (profile.experienceLevel) {
                    Difficulty.BEGINNER -> exercise.difficulty in listOf(Difficulty.BEGINNER, Difficulty.INTERMEDIATE)
                    Difficulty.INTERMEDIATE -> exercise.difficulty != Difficulty.EXPERT
                    else -> true
                }
            }
            .filter { exercise ->
                exercise.id !in profile.excludedExercises
            }

        val filteredExercises = physicalLimitationFilter.filterExercises(
            exercises = availableExercises,
            limitations = profile.physicalLimitations
        )

        val selected = mutableListOf<ProgramExercise>()
        val usedExerciseIds = mutableSetOf<String>()

        // Prioritize compounds for strength-focused days
        val compounds = filteredExercises.filter { it.type == ExerciseType.COMPOUND }
        val isolations = filteredExercises.filter { it.type == ExerciseType.ISOLATION }

        // Select exercises for each target muscle
        val exercisesPerMuscle = (exerciseCount / targetMuscles.size).coerceAtLeast(1)
        val extraExercises = exerciseCount % targetMuscles.size

        targetMuscles.forEachIndexed { index, muscle ->
            val muscleExercises = if (isStrengthFocused) {
                compounds.filter { muscle in it.primaryMuscles } +
                    isolations.filter { muscle in it.primaryMuscles }
            } else {
                (compounds.filter { muscle in it.primaryMuscles } +
                    isolations.filter { muscle in it.primaryMuscles }).shuffled()
            }

            val count = exercisesPerMuscle + if (index < extraExercises) 1 else 0

            muscleExercises
                .filter { it.id !in usedExerciseIds }
                .take(count)
                .forEach { exercise ->
                    usedExerciseIds.add(exercise.id)
                    selected.add(
                        createProgramExercise(
                            exercise = exercise,
                            goal = goal,
                            isStrengthFocused = isStrengthFocused,
                            isFirstExercise = selected.isEmpty()
                        )
                    )
                }
        }

        return selected.take(exerciseCount)
    }

    private fun createProgramExercise(
        exercise: Exercise,
        goal: FitnessGoal,
        isStrengthFocused: Boolean,
        isFirstExercise: Boolean
    ): ProgramExercise {
        val (sets, repRange, rpeRange, rest) = when {
            isStrengthFocused && exercise.type == ExerciseType.COMPOUND -> {
                if (isFirstExercise) {
                    Quad(5, 3..5, 8..9, 180)
                } else {
                    Quad(4, 5..5, 7..8, 150)
                }
            }
            goal == FitnessGoal.BUILD_STRENGTH -> Quad(4, 4..6, 7..9, 150)
            goal == FitnessGoal.BUILD_MUSCLE -> {
                if (exercise.type == ExerciseType.COMPOUND) {
                    Quad(4, 6..10, 7..8, 120)
                } else {
                    Quad(3, 10..15, 7..8, 60)
                }
            }
            goal == FitnessGoal.LOSE_WEIGHT -> Quad(3, 12..15, 6..7, 45)
            goal == FitnessGoal.IMPROVE_ENDURANCE -> Quad(3, 15..20, 5..6, 30)
            goal == FitnessGoal.POWERBUILDING -> {
                if (exercise.type == ExerciseType.COMPOUND) {
                    Quad(4, 4..8, 7..9, 150)
                } else {
                    Quad(3, 8..12, 7..8, 90)
                }
            }
            else -> Quad(3, 8..12, 7..8, 90)
        }

        return ProgramExercise(
            exerciseId = exercise.id,
            name = exercise.name,
            sets = sets,
            reps = repRange,
            rpeRange = rpeRange,
            restSeconds = rest
        )
    }

    // ========================================================================
    // PROGRESSION RULES
    // ========================================================================

    private fun generateProgressionRules(
        goal: FitnessGoal,
        periodization: PeriodizationType
    ): ProgressionRules {
        val trigger = when (periodization) {
            PeriodizationType.LINEAR -> ProgressionTrigger.ALL_SETS_COMPLETED
            PeriodizationType.DOUBLE_PROGRESSION -> ProgressionTrigger.ALL_SETS_COMPLETED
            PeriodizationType.WAVE -> ProgressionTrigger.TOP_SET_HIT
            PeriodizationType.DAILY_UNDULATING -> ProgressionTrigger.RPE_BASED
            PeriodizationType.BLOCK -> ProgressionTrigger.ALL_SETS_COMPLETED
            else -> ProgressionTrigger.ALL_SETS_COMPLETED
        }

        val deloadPct = when (goal) {
            FitnessGoal.BUILD_STRENGTH -> 0.10f
            FitnessGoal.BUILD_MUSCLE -> 0.15f
            else -> 0.10f
        }

        return ProgressionRules(
            progressionTrigger = trigger,
            failureThreshold = if (goal == FitnessGoal.BUILD_STRENGTH) 2 else 3,
            deloadPercentage = deloadPct
        )
    }

    // ========================================================================
    // PROGRAM NAMING AND DESCRIPTION
    // ========================================================================

    private fun generateProgramName(goal: FitnessGoal, split: WorkoutSplit): String {
        val goalPart = when (goal) {
            FitnessGoal.BUILD_STRENGTH -> "Strength Builder"
            FitnessGoal.BUILD_MUSCLE -> "Hypertrophy"
            FitnessGoal.LOSE_WEIGHT -> "Fat Loss"
            FitnessGoal.IMPROVE_ENDURANCE -> "Endurance"
            FitnessGoal.POWERBUILDING -> "Powerbuilding"
            FitnessGoal.ATHLETIC_PERFORMANCE -> "Athletic Performance"
            else -> "General Fitness"
        }
        return "AI $goalPart - ${split.displayName}"
    }

    private fun generateProgramDescription(
        params: AIProgramGeneratorParams,
        split: WorkoutSplit,
        periodization: PeriodizationType
    ): String {
        val emphasisNote = if (params.emphasisMuscles.isNotEmpty()) {
            " with emphasis on ${params.emphasisMuscles.joinToString(", ") { it.displayName }}."
        } else "."

        return "AI-generated ${params.durationWeeks}-week ${split.displayName} program for ${params.primaryGoal.displayName.lowercase()}. " +
            "Uses ${periodization.displayName.lowercase()} progression${emphasisNote} " +
            "${params.daysPerWeek} training days per week" +
            if (params.includeDeloads) " with deload every ${params.deloadFrequency} weeks." else "."
    }

    // ========================================================================
    // REASONING EXPLANATIONS
    // ========================================================================

    private fun getSplitSelectionReason(split: WorkoutSplit, daysPerWeek: Int, goal: FitnessGoal): String {
        return when (split) {
            WorkoutSplit.FULL_BODY -> "Full body selected for $daysPerWeek days/week to maximize training frequency for each muscle group."
            WorkoutSplit.UPPER_LOWER -> "Upper/Lower split selected for optimal balance with $daysPerWeek training days, allowing adequate recovery."
            WorkoutSplit.PUSH_PULL_LEGS -> "Push/Pull/Legs selected to organize movements by pattern, ideal for ${goal.displayName.lowercase()}."
            WorkoutSplit.ARNOLD_SPLIT -> "Arnold split selected for high-frequency training with antagonist muscle pairing."
            WorkoutSplit.BRO_SPLIT -> "Body part split selected for focused training of individual muscle groups."
            WorkoutSplit.POWERLIFTING -> "Powerlifting split selected to prioritize the big three lifts."
            WorkoutSplit.CUSTOM -> "Custom split based on your preferences."
        }
    }

    private fun getPeriodizationReason(
        periodization: PeriodizationType,
        goal: FitnessGoal,
        experience: Difficulty
    ): String {
        return when (periodization) {
            PeriodizationType.LINEAR -> "Linear progression selected for ${experience.name.lowercase()} level - simple, effective, and easy to track."
            PeriodizationType.DOUBLE_PROGRESSION -> "Double progression selected for ${goal.displayName.lowercase()} - increase reps then weight for optimal hypertrophy."
            PeriodizationType.WAVE -> "Wave loading selected for ${experience.name.lowercase()} strength training - cycling intensity for sustained progress."
            PeriodizationType.DAILY_UNDULATING -> "Daily undulating periodization selected for advanced ${goal.displayName.lowercase()} - varying stimulus for continued adaptation."
            PeriodizationType.BLOCK -> "Block periodization selected for advanced strength training - focused phases for peak performance."
            else -> "Progression style matched to your goal and experience level."
        }
    }

    private fun getVolumeDistributionReason(days: List<ProgramDay>, goal: FitnessGoal): String {
        val totalExercises = days.sumOf { it.exercises.size }
        val avgPerDay = totalExercises / days.size
        return "Volume distributed across ${days.size} days averaging $avgPerDay exercises per session, " +
            "optimized for ${goal.displayName.lowercase()} while allowing adequate recovery."
    }

    private fun getProgressionStrategyReason(periodization: PeriodizationType, goal: FitnessGoal): String {
        return when (periodization) {
            PeriodizationType.LINEAR -> "Add weight each session when all sets and reps are completed."
            PeriodizationType.DOUBLE_PROGRESSION -> "Hit the top of rep range across all sets, then increase weight and drop to bottom of rep range."
            PeriodizationType.WAVE -> "Cycle through light, medium, and heavy weeks before increasing baseline weights."
            PeriodizationType.DAILY_UNDULATING -> "Vary intensity daily (strength/hypertrophy/power) with RPE-based autoregulation."
            PeriodizationType.BLOCK -> "Focus on accumulation (volume) then intensification (intensity) phases."
            else -> "Progress based on successful completion of prescribed sets and reps."
        }
    }

    private fun getLimitationAdjustments(limitations: List<UserLimitation>): List<String> {
        if (limitations.isEmpty()) return emptyList()

        return limitations.map { userLimitation ->
            physicalLimitationFilter.getExclusionReason(userLimitation.limitation)
        }
    }
}

/**
 * Helper class for bundling exercise parameters
 */
private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
