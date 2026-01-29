package com.workoutgen.domain.model

import java.util.UUID

// ============================================================================
// ENUMS - Core classification types
// ============================================================================

enum class MuscleGroup(val displayName: String) {
    CHEST("Chest"),
    BACK("Back"),
    SHOULDERS("Shoulders"),
    BICEPS("Biceps"),
    TRICEPS("Triceps"),
    FOREARMS("Forearms"),
    QUADRICEPS("Quadriceps"),
    HAMSTRINGS("Hamstrings"),
    GLUTES("Glutes"),
    CALVES("Calves"),
    CORE("Core"),
    FULL_BODY("Full Body")
}

enum class Equipment(val displayName: String) {
    NONE("Bodyweight"),
    DUMBBELLS("Dumbbells"),
    BARBELL("Barbell"),
    KETTLEBELL("Kettlebell"),
    RESISTANCE_BANDS("Resistance Bands"),
    PULL_UP_BAR("Pull-up Bar"),
    BENCH("Bench"),
    CABLE_MACHINE("Cable Machine"),
    SMITH_MACHINE("Smith Machine"),
    LEG_PRESS("Leg Press"),
    LAT_PULLDOWN("Lat Pulldown"),
    ROWING_MACHINE("Rowing Machine"),
    TREADMILL("Treadmill"),
    STATIONARY_BIKE("Stationary Bike"),
    FOAM_ROLLER("Foam Roller"),
    DIP_STATION("Dip Station"),
    EZ_CURL_BAR("EZ Curl Bar"),
    TRAP_BAR("Trap Bar"),
    LANDMINE("Landmine"),
    MEDICINE_BALL("Medicine Ball"),
    BATTLE_ROPES("Battle Ropes"),
    BOX("Plyo Box"),
    SUSPENSION_TRAINER("Suspension Trainer (TRX)")
}

enum class ExerciseType {
    COMPOUND,
    ISOLATION,
    CARDIO,
    PLYOMETRIC,
    ISOMETRIC,
    STRETCHING,
    MOBILITY
}

enum class Difficulty {
    BEGINNER, INTERMEDIATE, ADVANCED, EXPERT
}

enum class FitnessGoal(val displayName: String, val description: String) {
    BUILD_MUSCLE("Build Muscle", "Hypertrophy-focused training with moderate weight and higher volume"),
    LOSE_WEIGHT("Lose Weight", "Fat loss through metabolic conditioning and caloric burn"),
    BUILD_STRENGTH("Build Strength", "Powerlifting-style training with heavy weights and lower reps"),
    IMPROVE_ENDURANCE("Improve Endurance", "Cardiovascular fitness and muscular endurance"),
    INCREASE_FLEXIBILITY("Increase Flexibility", "Stretching and mobility work"),
    GENERAL_FITNESS("General Fitness", "Balanced approach to overall health"),
    ATHLETIC_PERFORMANCE("Athletic Performance", "Sport-specific power and agility"),
    POWERBUILDING("Powerbuilding", "Combination of strength and hypertrophy")
}

enum class WorkoutSplit(val displayName: String, val daysPerWeek: IntRange) {
    FULL_BODY("Full Body", 2..4),
    UPPER_LOWER("Upper/Lower", 3..5),
    PUSH_PULL_LEGS("Push/Pull/Legs", 3..6),
    BRO_SPLIT("Body Part Split", 4..6),
    ARNOLD_SPLIT("Arnold Split", 5..6),
    POWERLIFTING("Powerlifting", 3..5),
    CUSTOM("Custom", 1..7)
}

enum class PeriodizationType(val displayName: String) {
    LINEAR("Linear Progression"),
    DOUBLE_PROGRESSION("Double Progression"),
    WAVE("Wave Loading"),
    BLOCK("Block Periodization"),
    DAILY_UNDULATING("Daily Undulating (DUP)"),
    WEEKLY_UNDULATING("Weekly Undulating"),
    DELOAD("Deload Week")
}

enum class SetType {
    WORKING, WARMUP, DROPSET, BACKOFF, AMRAP, REST_PAUSE, CLUSTER, MTOR, FAILURE
}

enum class ForceType {
    PUSH, PULL, LEGS, HINGE, CARRY, ROTATION
}

enum class Gender { MALE, FEMALE, OTHER }

enum class PRType {
    WEIGHT, REPS, VOLUME, ESTIMATED_1RM
}

enum class RecoveryStatus {
    RECOVERED, RECOVERING, FATIGUED, OVERTRAINED
}

enum class ProgressionTrigger {
    ALL_SETS_COMPLETED, TOP_SET_HIT, RPE_BASED, AMRAP_BASED
}

// ============================================================================
// PHYSICAL LIMITATIONS
// ============================================================================

/**
 * Common physical limitations that affect exercise selection
 */
enum class PhysicalLimitation(val displayName: String, val description: String) {
    // Knee-related
    ACL_REPAIR("ACL Repair", "Anterior cruciate ligament repair - avoid high-impact and deep knee flexion"),
    MENISCUS_REPAIR("Meniscus Repair", "Meniscus surgery recovery - avoid deep squats and twisting"),
    KNEE_ARTHRITIS("Knee Arthritis", "Degenerative knee condition - avoid high-impact activities"),
    PATELLA_TENDINITIS("Patella Tendinitis", "Jumper's knee - avoid jumping and deep knee bends"),

    // Shoulder-related
    SHOULDER_BURSITIS("Shoulder Bursitis", "Shoulder inflammation - avoid overhead movements"),
    ROTATOR_CUFF_INJURY("Rotator Cuff Injury", "Rotator cuff damage - avoid overhead press and lateral raises"),
    SHOULDER_IMPINGEMENT("Shoulder Impingement", "Shoulder impingement - avoid overhead and behind-neck movements"),
    LABRUM_TEAR("Labrum Tear", "Shoulder labrum tear - avoid extreme ranges of motion"),
    FROZEN_SHOULDER("Frozen Shoulder", "Adhesive capsulitis - limited range of motion"),

    // Back-related
    LOWER_BACK_PAIN("Lower Back Pain", "Chronic lumbar issues - avoid heavy spinal loading"),
    HERNIATED_DISC("Herniated Disc", "Disc herniation - avoid flexion under load"),
    SCIATICA("Sciatica", "Sciatic nerve irritation - avoid forward bending exercises"),
    SPONDYLOLISTHESIS("Spondylolisthesis", "Vertebral slippage - avoid hyperextension"),

    // Hip-related
    HIP_BURSITIS("Hip Bursitis", "Hip inflammation - avoid lateral leg movements"),
    HIP_LABRUM_TEAR("Hip Labrum Tear", "Hip labrum damage - avoid deep hip flexion"),
    HIP_REPLACEMENT("Hip Replacement", "Hip replacement surgery - avoid extreme ranges of motion"),

    // Other joints
    TENNIS_ELBOW("Tennis Elbow", "Lateral epicondylitis - avoid grip-intensive exercises"),
    GOLFERS_ELBOW("Golfer's Elbow", "Medial epicondylitis - avoid wrist flexion exercises"),
    CARPAL_TUNNEL("Carpal Tunnel", "Wrist nerve compression - avoid heavy gripping"),
    ANKLE_INSTABILITY("Ankle Instability", "Chronic ankle sprains - avoid unstable surfaces"),
    PLANTAR_FASCIITIS("Plantar Fasciitis", "Foot pain - avoid high-impact and prolonged standing"),

    // General conditions
    OSTEOPOROSIS("Osteoporosis", "Bone density loss - avoid high-impact and spinal flexion"),
    PREGNANCY("Pregnancy", "Exercise modifications for pregnancy"),
    HIGH_BLOOD_PRESSURE("High Blood Pressure", "Hypertension - avoid heavy isometric holds")
}

/**
 * Severity level of a physical limitation
 */
enum class LimitationSeverity(val displayName: String, val description: String) {
    MILD("Mild", "Minor discomfort - some exercises may need modification"),
    MODERATE("Moderate", "Noticeable limitation - avoid certain exercise categories"),
    SEVERE("Severe", "Significant restriction - strictly avoid all related exercises")
}

/**
 * Represents a user's physical limitation with severity and optional notes
 */
data class UserLimitation(
    val limitation: PhysicalLimitation,
    val severity: LimitationSeverity,
    val notes: String? = null,
    val diagnosedDate: Long? = null
)

// ============================================================================
// AI LEARNING DATA - For personalized workout generation
// ============================================================================

/**
 * Tracks user's exercise preferences learned over time
 */
data class ExercisePreference(
    val exerciseId: String,
    val timesPerformed: Int = 0,
    val timesSwapped: Int = 0,
    val averageRPE: Float? = null,
    val averageCompletionRate: Float = 1f,  // % of sets completed as prescribed
    val lastPerformed: Long? = null,
    val isLiked: Boolean? = null,  // Explicit user preference
    val swapReasons: List<String> = emptyList()  // Why user swapped away
)

/**
 * Tracks user's muscle group training patterns
 */
data class MuscleTrainingPattern(
    val muscleGroup: MuscleGroup,
    val preferredExercises: List<String> = emptyList(),
    val averageWeeklyVolume: Float = 0f,
    val recoveryRate: Float = 1f,  // How quickly user recovers (1.0 = average)
    val strengthProgression: Float = 0f  // Weekly strength gain %
)

/**
 * AI-generated training recommendations
 */
data class AIRecommendation(
    val type: RecommendationType,
    val message: String,
    val confidence: Float,  // 0-1 confidence score
    val data: Map<String, Any> = emptyMap()
)

enum class RecommendationType {
    INCREASE_VOLUME,
    DECREASE_VOLUME,
    ADD_REST_DAY,
    FOCUS_WEAK_POINT,
    DELOAD_RECOMMENDED,
    CHANGE_REP_RANGE,
    TRY_NEW_EXERCISE,
    FORM_CHECK_SUGGESTED
}

// ============================================================================
// CORE DATA CLASSES
// ============================================================================

data class Exercise(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val instructions: List<String>,
    val primaryMuscles: List<MuscleGroup>,
    val secondaryMuscles: List<MuscleGroup> = emptyList(),
    val equipmentRequired: List<Equipment>,
    val type: ExerciseType,
    val difficulty: Difficulty,
    val videoUrl: String? = null,
    val imageUrl: String? = null,
    val tips: List<String> = emptyList(),
    val alternatives: List<String> = emptyList(),
    val isCompound: Boolean = type == ExerciseType.COMPOUND,
    val isUnilateral: Boolean = false,
    val forceType: ForceType = ForceType.PUSH
)

data class WarmupSet(
    val setNumber: Int,
    val reps: Int,
    val percentOfWorking: Float,
    val weight: Float? = null
)

data class WorkoutExercise(
    val exercise: Exercise,
    val sets: Int,
    val reps: IntRange,
    val restSeconds: Int,
    val weight: Float? = null,
    val rpe: Int? = null,
    val rir: Int? = null,
    val tempo: String? = null,
    val notes: String? = null,
    val isSuperset: Boolean = false,
    val supersetGroupId: String? = null,
    val setType: SetType = SetType.WORKING,
    val warmupSets: List<WarmupSet> = emptyList()
)

data class Workout(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val exercises: List<WorkoutExercise>,
    val targetMuscles: List<MuscleGroup>,
    val estimatedDurationMinutes: Int,
    val difficulty: Difficulty,
    val goal: FitnessGoal,
    val warmup: List<WorkoutExercise> = emptyList(),
    val cooldown: List<WorkoutExercise> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val aiGenerated: Boolean = true,
    val aiReasoning: String? = null  // Why AI selected these exercises
)

// ============================================================================
// USER PROFILE
// ============================================================================

data class UserProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val experienceLevel: Difficulty = Difficulty.BEGINNER,
    val fitnessGoals: List<FitnessGoal> = listOf(FitnessGoal.GENERAL_FITNESS),
    val availableEquipment: Set<Equipment> = setOf(Equipment.NONE),
    val preferredWorkoutDuration: Int = 45,
    val workoutDaysPerWeek: Int = 3,
    val preferredSplit: WorkoutSplit = WorkoutSplit.FULL_BODY,
    val physicalLimitations: List<UserLimitation> = emptyList(),
    val excludedExercises: List<String> = emptyList(),
    val useMetricUnits: Boolean = true,
    val bodyWeight: Float? = null,
    val height: Float? = null,
    val age: Int? = null,
    val gender: Gender? = null,
    val maxes: Map<String, Float> = emptyMap(),  // Known 1RMs for exercises
    // AI Learning preferences
    val preferredProgressionStyle: PeriodizationType = PeriodizationType.DOUBLE_PROGRESSION,
    val focusAreas: List<MuscleGroup> = emptyList(),  // Muscles to emphasize
    val weakPoints: List<MuscleGroup> = emptyList(),   // AI-detected weak areas
    val exercisePreferences: Map<String, Float> = emptyMap(),  // exerciseId -> preference score
    // Program tracking
    val currentProgramId: String? = null,
    val programStartDate: Long? = null,
    val currentWeek: Int = 1,
    val currentDay: Int = 1
)

data class WorkoutGenerationParams(
    val userProfile: UserProfile,
    val targetMuscles: List<MuscleGroup>? = null,
    val durationMinutes: Int? = null,
    val includeWarmup: Boolean = true,
    val includeCooldown: Boolean = true,
    val prioritizeCompound: Boolean = true,
    val maxExercises: Int = 8,
    val periodization: PeriodizationType? = null,
    val intensityLevel: Float = 1f  // 0.5 = easy day, 1.0 = normal, 1.2 = push hard
)

// ============================================================================
// WORKOUT LOGGING
// ============================================================================

data class WorkoutLog(
    val id: String = UUID.randomUUID().toString(),
    val workoutId: String,
    val workoutName: String,
    val startTime: Long,
    val endTime: Long? = null,
    val exerciseLogs: List<ExerciseLog> = emptyList(),
    val notes: String? = null,
    val rating: Int? = null,  // 1-5 user rating
    val mood: String? = null,
    val energyLevel: Int? = null,  // 1-5 before workout
    val sleepQuality: Int? = null,  // 1-5 previous night
    val totalVolume: Float = 0f,
    val targetMuscles: List<MuscleGroup> = emptyList(),
    val exerciseSwaps: List<ExerciseSwapLog> = emptyList(),
    val adjustmentsMade: Int = 0  // Number of weight/rep adjustments during workout
)

/**
 * Tracks exercise swaps during a workout for AI learning
 */
data class ExerciseSwapLog(
    val originalExerciseId: String,
    val newExerciseId: String,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ExerciseLog(
    val exerciseId: String,
    val exerciseName: String,
    val sets: List<SetLog>,
    val notes: String? = null,
    val personalRecord: Boolean = false,
    val wasSwapped: Boolean = false,  // Was this exercise swapped in?
    val swapReason: String? = null
)

data class SetLog(
    val setNumber: Int,
    val targetReps: Int,  // What was prescribed
    val reps: Int,        // What was achieved
    val weight: Float?,
    val rpe: Int? = null,
    val rir: Int? = null,
    val isWarmup: Boolean = false,
    val isDropSet: Boolean = false,
    val isFailure: Boolean = false,
    val setType: SetType = SetType.WORKING,
    val failureReason: String? = null,  // Why reps weren't completed
    val notes: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

// ============================================================================
// ANALYTICS & PROGRESS
// ============================================================================

data class PersonalRecord(
    val exerciseId: String,
    val exerciseName: String,
    val type: PRType,
    val value: Float,
    val reps: Int? = null,
    val date: Long,
    val estimated1RM: Float? = null
)

data class MuscleRecovery(
    val muscleGroup: MuscleGroup,
    val lastWorked: Long,
    val volumeInLastSession: Float,
    val recoveryStatus: RecoveryStatus,
    val estimatedRecoveryTime: Int
)

data class WeeklyVolume(
    val weekStartDate: Long,
    val muscleVolumes: Map<MuscleGroup, VolumeData>
)

data class VolumeData(
    val sets: Int,
    val reps: Int,
    val totalWeight: Float,
    val workouts: Int
)

data class BodyMeasurement(
    val id: String = UUID.randomUUID().toString(),
    val date: Long = System.currentTimeMillis(),
    val weight: Float? = null,
    val bodyFatPercentage: Float? = null,
    val chest: Float? = null,
    val waist: Float? = null,
    val hips: Float? = null,
    val bicepLeft: Float? = null,
    val bicepRight: Float? = null,
    val thighLeft: Float? = null,
    val thighRight: Float? = null,
    val calfLeft: Float? = null,
    val calfRight: Float? = null,
    val neck: Float? = null,
    val shoulders: Float? = null,
    val notes: String? = null
)

// ============================================================================
// TRAINING PROGRAMS
// ============================================================================

data class TrainingProgram(
    val id: String,
    val name: String,
    val description: String,
    val author: String? = null,
    val difficulty: Difficulty,
    val goals: List<FitnessGoal>,
    val durationWeeks: Int = 0,
    val daysPerWeek: Int,
    val split: WorkoutSplit,
    val periodization: PeriodizationType = PeriodizationType.LINEAR,
    val requiredEquipment: Set<Equipment> = emptySet(),
    val deloadFrequency: Int = 4,
    val programDays: List<ProgramDay>,
    val progressionRules: ProgressionRules = ProgressionRules()
)

data class ProgramDay(
    val dayNumber: Int,
    val name: String,
    val targetMuscles: List<MuscleGroup>,
    val exercises: List<ProgramExercise>
)

data class ProgramExercise(
    val exerciseId: String,
    val name: String,
    val sets: Int,
    val reps: IntRange,
    val rpeRange: IntRange? = null,
    val rirRange: IntRange? = null,
    val restSeconds: Int = 90,
    val notes: String? = null,
    val setType: SetType = SetType.WORKING
)

data class ProgressionRules(
    val weightIncrementKg: Map<MuscleGroup, Float> = mapOf(
        MuscleGroup.QUADRICEPS to 2.5f,
        MuscleGroup.BACK to 2.5f,
        MuscleGroup.CHEST to 2.5f,
        MuscleGroup.SHOULDERS to 1.25f,
        MuscleGroup.BICEPS to 1.25f,
        MuscleGroup.TRICEPS to 1.25f,
        MuscleGroup.HAMSTRINGS to 2.5f,
        MuscleGroup.GLUTES to 2.5f,
        MuscleGroup.CALVES to 2.5f,
        MuscleGroup.CORE to 1.25f,
        MuscleGroup.FOREARMS to 1.25f
    ),
    val progressionTrigger: ProgressionTrigger = ProgressionTrigger.ALL_SETS_COMPLETED,
    val failureThreshold: Int = 2,
    val deloadPercentage: Float = 0.1f
)

// ============================================================================
// UTILITY CALCULATORS
// ============================================================================

object OneRepMaxCalculator {
    fun brzycki(weight: Float, reps: Int): Float {
        if (reps == 1) return weight
        if (reps > 12) return epley(weight, reps)
        return weight * (36f / (37f - reps))
    }
    
    fun epley(weight: Float, reps: Int): Float {
        if (reps == 1) return weight
        return weight * (1 + reps / 30f)
    }
    
    fun lander(weight: Float, reps: Int): Float {
        if (reps == 1) return weight
        return weight * 100f / (101.3f - 2.67123f * reps)
    }
    
    fun calculate(weight: Float, reps: Int): Float {
        if (reps == 1) return weight
        return (brzycki(weight, reps) + epley(weight, reps) + lander(weight, reps)) / 3f
    }
    
    fun weightForReps(oneRepMax: Float, targetReps: Int): Float {
        if (targetReps == 1) return oneRepMax
        return oneRepMax * (37f - targetReps) / 36f
    }
    
    fun percentageOf1RM(oneRepMax: Float, percentage: Float): Float {
        return oneRepMax * percentage
    }
}

object WarmupCalculator {
    fun generateWarmupSets(workingWeight: Float, workingReps: Int): List<WarmupSet> {
        if (workingWeight <= 20f) {
            return listOf(WarmupSet(1, 10, 0.5f, workingWeight * 0.5f))
        }
        return listOf(
            WarmupSet(1, 10, 0f, 0f),
            WarmupSet(2, 8, 0.4f, workingWeight * 0.4f),
            WarmupSet(3, 5, 0.6f, workingWeight * 0.6f),
            WarmupSet(4, 3, 0.8f, workingWeight * 0.8f)
        )
    }
}

object VolumeCalculator {
    fun totalVolume(logs: List<SetLog>): Float {
        return logs.filter { !it.isWarmup }
            .sumOf { (it.reps * (it.weight ?: 0f)).toDouble() }
            .toFloat()
    }
    
    fun effectiveSets(logs: List<SetLog>): Int {
        return logs.count { !it.isWarmup && it.reps >= 5 }
    }
}

object RecoveryEstimator {
    private const val BASE_RECOVERY_HOURS = 48

    fun estimateRecovery(
        muscleGroup: MuscleGroup,
        sets: Int,
        averageRPE: Float,
        userExperience: Difficulty
    ): Int {
        var hours = BASE_RECOVERY_HOURS
        hours += (sets - 10) * 4
        hours += ((averageRPE - 7) * 8).toInt()

        hours = when (userExperience) {
            Difficulty.BEGINNER -> (hours * 1.2).toInt()
            Difficulty.INTERMEDIATE -> hours
            Difficulty.ADVANCED -> (hours * 0.9).toInt()
            Difficulty.EXPERT -> (hours * 0.8).toInt()
        }

        hours = when (muscleGroup) {
            MuscleGroup.QUADRICEPS, MuscleGroup.BACK, MuscleGroup.HAMSTRINGS -> (hours * 1.2).toInt()
            MuscleGroup.BICEPS, MuscleGroup.TRICEPS, MuscleGroup.CALVES -> (hours * 0.8).toInt()
            else -> hours
        }

        return hours.coerceIn(24, 120)
    }
}

// ============================================================================
// AI PROGRAM GENERATION
// ============================================================================

/**
 * Input parameters for AI program generation
 */
data class AIProgramGeneratorParams(
    val daysPerWeek: Int,
    val primaryGoal: FitnessGoal,
    val durationWeeks: Int = 12,
    val programName: String? = null,
    val emphasisMuscles: List<MuscleGroup> = emptyList(),
    val preferredSplit: WorkoutSplit? = null,
    val includeDeloads: Boolean = true,
    val deloadFrequency: Int = 4
)

/**
 * AI explanation of choices made during program generation
 */
data class ProgramReasoning(
    val splitSelection: String,
    val periodizationSelection: String,
    val volumeDistribution: String,
    val progressionStrategy: String,
    val limitationAdjustments: List<String> = emptyList()
)

/**
 * Wrapper for generated program with reasoning
 */
data class AIGeneratedProgram(
    val program: TrainingProgram,
    val reasoning: ProgramReasoning,
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * Result type for program generation
 */
sealed class GeneratedProgram {
    data class Success(val result: AIGeneratedProgram) : GeneratedProgram()
    data class Error(val message: String) : GeneratedProgram()
}

/**
 * Context for generating a workout within an active program
 */
data class ProgramWorkoutContext(
    val programId: String,
    val currentDay: ProgramDay,
    val currentWeek: Int,
    val isDeloadWeek: Boolean
)
