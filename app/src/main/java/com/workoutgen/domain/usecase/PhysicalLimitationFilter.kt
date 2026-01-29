package com.workoutgen.domain.usecase

import com.workoutgen.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Filters exercises based on user's physical limitations.
 *
 * This class maps physical limitations to exercise exclusion criteria,
 * considering limitation severity to determine how strictly to filter.
 */
@Singleton
class PhysicalLimitationFilter @Inject constructor() {

    /**
     * Filter a list of exercises based on user's physical limitations.
     *
     * @param exercises The full list of available exercises
     * @param limitations The user's physical limitations with severity
     * @return Filtered list of safe exercises
     */
    fun filterExercises(
        exercises: List<Exercise>,
        limitations: List<UserLimitation>
    ): List<Exercise> {
        if (limitations.isEmpty()) return exercises

        return exercises.filter { exercise ->
            limitations.all { userLimitation ->
                isExerciseSafe(exercise, userLimitation)
            }
        }
    }

    /**
     * Check if a specific exercise is safe for a given limitation.
     */
    fun isExerciseSafe(exercise: Exercise, userLimitation: UserLimitation): Boolean {
        val exclusions = getExclusionCriteria(userLimitation.limitation, userLimitation.severity)

        // Check exercise type exclusions
        if (exercise.type in exclusions.excludedTypes) {
            return false
        }

        // Check muscle group exclusions
        val affectedMuscles = exercise.primaryMuscles + exercise.secondaryMuscles
        if (affectedMuscles.any { it in exclusions.excludedMuscles }) {
            return false
        }

        // Check exercise name patterns (case-insensitive)
        val nameLower = exercise.name.lowercase()
        if (exclusions.excludedNamePatterns.any { pattern -> nameLower.contains(pattern.lowercase()) }) {
            return false
        }

        // Check force type exclusions
        if (exercise.forceType in exclusions.excludedForceTypes) {
            return false
        }

        return true
    }

    /**
     * Get exercises that should be avoided for a limitation (for UI display).
     */
    fun getExcludedExercises(
        exercises: List<Exercise>,
        limitation: UserLimitation
    ): List<Exercise> {
        return exercises.filter { !isExerciseSafe(it, limitation) }
    }

    /**
     * Get a human-readable explanation of why exercises are excluded.
     */
    fun getExclusionReason(limitation: PhysicalLimitation): String {
        return when (limitation) {
            PhysicalLimitation.ACL_REPAIR -> "Avoiding high-impact movements, jumping, and deep knee flexion exercises"
            PhysicalLimitation.MENISCUS_REPAIR -> "Avoiding deep squats, twisting movements, and high knee stress exercises"
            PhysicalLimitation.KNEE_ARTHRITIS -> "Avoiding high-impact and heavy knee loading exercises"
            PhysicalLimitation.PATELLA_TENDINITIS -> "Avoiding jumping and exercises with deep knee bends"
            PhysicalLimitation.SHOULDER_BURSITIS -> "Avoiding overhead movements and lateral raises"
            PhysicalLimitation.ROTATOR_CUFF_INJURY -> "Avoiding overhead press and exercises with arm rotation under load"
            PhysicalLimitation.SHOULDER_IMPINGEMENT -> "Avoiding overhead and behind-the-neck movements"
            PhysicalLimitation.LABRUM_TEAR -> "Avoiding extreme shoulder range of motion exercises"
            PhysicalLimitation.FROZEN_SHOULDER -> "Limiting exercises requiring full shoulder mobility"
            PhysicalLimitation.LOWER_BACK_PAIN -> "Avoiding heavy spinal loading and forward bending under load"
            PhysicalLimitation.HERNIATED_DISC -> "Avoiding spinal flexion and heavy deadlifts"
            PhysicalLimitation.SCIATICA -> "Avoiding forward bending and heavy hip hinge movements"
            PhysicalLimitation.SPONDYLOLISTHESIS -> "Avoiding hyperextension and heavy axial loading"
            PhysicalLimitation.HIP_BURSITIS -> "Avoiding lateral leg movements and hip abduction exercises"
            PhysicalLimitation.HIP_LABRUM_TEAR -> "Avoiding deep hip flexion and extreme hip rotation"
            PhysicalLimitation.HIP_REPLACEMENT -> "Avoiding extreme hip range of motion and high impact"
            PhysicalLimitation.TENNIS_ELBOW -> "Avoiding grip-intensive and wrist extension exercises"
            PhysicalLimitation.GOLFERS_ELBOW -> "Avoiding wrist flexion and forearm exercises"
            PhysicalLimitation.CARPAL_TUNNEL -> "Avoiding heavy gripping and wrist stress exercises"
            PhysicalLimitation.ANKLE_INSTABILITY -> "Avoiding unstable surfaces and single-leg balance exercises"
            PhysicalLimitation.PLANTAR_FASCIITIS -> "Avoiding high-impact and prolonged standing exercises"
            PhysicalLimitation.OSTEOPOROSIS -> "Avoiding high-impact activities and spinal flexion"
            PhysicalLimitation.PREGNANCY -> "Modifying exercises for safety during pregnancy"
            PhysicalLimitation.HIGH_BLOOD_PRESSURE -> "Avoiding heavy isometric holds and breath-holding exercises"
        }
    }

    /**
     * Get exclusion criteria for a limitation based on severity.
     */
    private fun getExclusionCriteria(
        limitation: PhysicalLimitation,
        severity: LimitationSeverity
    ): ExclusionCriteria {
        val baseCriteria = getBaseExclusionCriteria(limitation)

        // Adjust based on severity
        return when (severity) {
            LimitationSeverity.MILD -> baseCriteria.copy(
                // Only exclude the most problematic patterns for mild
                excludedNamePatterns = baseCriteria.excludedNamePatterns.take(
                    (baseCriteria.excludedNamePatterns.size / 2).coerceAtLeast(1)
                )
            )
            LimitationSeverity.MODERATE -> baseCriteria
            LimitationSeverity.SEVERE -> baseCriteria.copy(
                // Add related muscle groups for severe limitations
                excludedMuscles = baseCriteria.excludedMuscles + getRelatedMuscles(limitation)
            )
        }
    }

    /**
     * Get base exclusion criteria for each limitation type.
     */
    private fun getBaseExclusionCriteria(limitation: PhysicalLimitation): ExclusionCriteria {
        return when (limitation) {
            // Knee-related limitations
            PhysicalLimitation.ACL_REPAIR -> ExclusionCriteria(
                excludedTypes = setOf(ExerciseType.PLYOMETRIC),
                excludedNamePatterns = listOf(
                    "jump", "lunge", "box jump", "burpee", "skip",
                    "hop", "bound", "pistol squat", "split squat",
                    "bulgarian", "plyometric", "explosive"
                ),
                excludedMuscles = emptySet(),
                excludedForceTypes = emptySet()
            )

            PhysicalLimitation.MENISCUS_REPAIR -> ExclusionCriteria(
                excludedTypes = setOf(ExerciseType.PLYOMETRIC),
                excludedNamePatterns = listOf(
                    "deep squat", "full squat", "ass to grass", "atg",
                    "lunge", "jump", "twist", "pivot", "sissy squat"
                ),
                excludedMuscles = emptySet(),
                excludedForceTypes = emptySet()
            )

            PhysicalLimitation.KNEE_ARTHRITIS -> ExclusionCriteria(
                excludedTypes = setOf(ExerciseType.PLYOMETRIC),
                excludedNamePatterns = listOf(
                    "jump", "run", "sprint", "bound", "hop",
                    "deep squat", "heavy squat", "lunge"
                ),
                excludedMuscles = emptySet(),
                excludedForceTypes = emptySet()
            )

            PhysicalLimitation.PATELLA_TENDINITIS -> ExclusionCriteria(
                excludedTypes = setOf(ExerciseType.PLYOMETRIC),
                excludedNamePatterns = listOf(
                    "jump", "squat", "lunge", "leg extension",
                    "box jump", "depth jump", "step up"
                ),
                excludedMuscles = emptySet(),
                excludedForceTypes = emptySet()
            )

            // Shoulder-related limitations
            PhysicalLimitation.SHOULDER_BURSITIS -> ExclusionCriteria(
                excludedTypes = emptySet(),
                excludedNamePatterns = listOf(
                    "overhead press", "military press", "shoulder press",
                    "lateral raise", "upright row", "behind neck",
                    "arnold press", "push press", "snatch"
                ),
                excludedMuscles = emptySet(),
                excludedForceTypes = emptySet()
            )

            PhysicalLimitation.ROTATOR_CUFF_INJURY -> ExclusionCriteria(
                excludedTypes = emptySet(),
                excludedNamePatterns = listOf(
                    "overhead press", "military press", "shoulder press",
                    "lateral raise", "upright row", "face pull",
                    "internal rotation", "external rotation", "snatch",
                    "clean and press", "behind neck"
                ),
                excludedMuscles = emptySet(),
                excludedForceTypes = emptySet()
            )

            PhysicalLimitation.SHOULDER_IMPINGEMENT -> ExclusionCriteria(
                excludedTypes = emptySet(),
                excludedNamePatterns = listOf(
                    "overhead", "military press", "behind neck",
                    "upright row", "lateral raise above shoulder",
                    "snatch", "push press"
                ),
                excludedMuscles = emptySet(),
                excludedForceTypes = emptySet()
            )

            PhysicalLimitation.LABRUM_TEAR -> ExclusionCriteria(
                excludedTypes = emptySet(),
                excludedNamePatterns = listOf(
                    "dip", "chest fly", "pec deck", "cable crossover",
                    "behind neck", "wide grip", "snatch"
                ),
                excludedMuscles = emptySet(),
                excludedForceTypes = emptySet()
            )

            PhysicalLimitation.FROZEN_SHOULDER -> ExclusionCriteria(
                excludedTypes = emptySet(),
                excludedNamePatterns = listOf(
                    "overhead", "pull up", "chin up", "lat pulldown",
                    "behind neck", "shoulder press", "lateral raise"
                ),
                excludedMuscles = setOf(MuscleGroup.SHOULDERS),
                excludedForceTypes = emptySet()
            )

            // Back-related limitations
            PhysicalLimitation.LOWER_BACK_PAIN -> ExclusionCriteria(
                excludedTypes = emptySet(),
                excludedNamePatterns = listOf(
                    "deadlift", "good morning", "bent over row",
                    "romanian deadlift", "stiff leg", "back extension",
                    "hyperextension", "barbell row"
                ),
                excludedMuscles = emptySet(),
                excludedForceTypes = setOf(ForceType.HINGE)
            )

            PhysicalLimitation.HERNIATED_DISC -> ExclusionCriteria(
                excludedTypes = emptySet(),
                excludedNamePatterns = listOf(
                    "deadlift", "squat", "good morning", "bent over",
                    "crunch", "sit up", "leg raise", "toe touch",
                    "romanian deadlift", "back extension"
                ),
                excludedMuscles = emptySet(),
                excludedForceTypes = setOf(ForceType.HINGE)
            )

            PhysicalLimitation.SCIATICA -> ExclusionCriteria(
                excludedTypes = emptySet(),
                excludedNamePatterns = listOf(
                    "deadlift", "good morning", "seated row",
                    "bent over", "toe touch", "forward fold",
                    "romanian deadlift"
                ),
                excludedMuscles = emptySet(),
                excludedForceTypes = setOf(ForceType.HINGE)
            )

            PhysicalLimitation.SPONDYLOLISTHESIS -> ExclusionCriteria(
                excludedTypes = emptySet(),
                excludedNamePatterns = listOf(
                    "back extension", "hyperextension", "superman",
                    "deadlift", "good morning", "squat"
                ),
                excludedMuscles = emptySet(),
                excludedForceTypes = emptySet()
            )

            // Hip-related limitations
            PhysicalLimitation.HIP_BURSITIS -> ExclusionCriteria(
                excludedTypes = emptySet(),
                excludedNamePatterns = listOf(
                    "side lying", "lateral", "hip abduction",
                    "clamshell", "fire hydrant", "side lunge"
                ),
                excludedMuscles = emptySet(),
                excludedForceTypes = emptySet()
            )

            PhysicalLimitation.HIP_LABRUM_TEAR -> ExclusionCriteria(
                excludedTypes = emptySet(),
                excludedNamePatterns = listOf(
                    "deep squat", "sumo", "wide stance", "frog",
                    "pigeon", "hip circle", "hurdle"
                ),
                excludedMuscles = emptySet(),
                excludedForceTypes = emptySet()
            )

            PhysicalLimitation.HIP_REPLACEMENT -> ExclusionCriteria(
                excludedTypes = setOf(ExerciseType.PLYOMETRIC),
                excludedNamePatterns = listOf(
                    "deep squat", "lunge", "hip rotation",
                    "cross leg", "figure four", "pigeon"
                ),
                excludedMuscles = emptySet(),
                excludedForceTypes = emptySet()
            )

            // Other joint limitations
            PhysicalLimitation.TENNIS_ELBOW -> ExclusionCriteria(
                excludedTypes = emptySet(),
                excludedNamePatterns = listOf(
                    "wrist extension", "reverse curl", "hammer curl",
                    "farmer walk", "dead hang", "pull up", "chin up"
                ),
                excludedMuscles = setOf(MuscleGroup.FOREARMS),
                excludedForceTypes = emptySet()
            )

            PhysicalLimitation.GOLFERS_ELBOW -> ExclusionCriteria(
                excludedTypes = emptySet(),
                excludedNamePatterns = listOf(
                    "wrist curl", "preacher curl", "chin up",
                    "pull up", "row"
                ),
                excludedMuscles = setOf(MuscleGroup.FOREARMS),
                excludedForceTypes = emptySet()
            )

            PhysicalLimitation.CARPAL_TUNNEL -> ExclusionCriteria(
                excludedTypes = emptySet(),
                excludedNamePatterns = listOf(
                    "wrist", "grip", "farmer", "deadlift",
                    "pull up", "chin up", "barbell"
                ),
                excludedMuscles = setOf(MuscleGroup.FOREARMS),
                excludedForceTypes = emptySet()
            )

            PhysicalLimitation.ANKLE_INSTABILITY -> ExclusionCriteria(
                excludedTypes = setOf(ExerciseType.PLYOMETRIC),
                excludedNamePatterns = listOf(
                    "single leg", "bosu", "balance board",
                    "pistol", "jump", "hop", "lateral"
                ),
                excludedMuscles = emptySet(),
                excludedForceTypes = emptySet()
            )

            PhysicalLimitation.PLANTAR_FASCIITIS -> ExclusionCriteria(
                excludedTypes = setOf(ExerciseType.PLYOMETRIC, ExerciseType.CARDIO),
                excludedNamePatterns = listOf(
                    "jump", "run", "sprint", "box jump",
                    "calf raise", "skip", "hop"
                ),
                excludedMuscles = setOf(MuscleGroup.CALVES),
                excludedForceTypes = emptySet()
            )

            // General conditions
            PhysicalLimitation.OSTEOPOROSIS -> ExclusionCriteria(
                excludedTypes = setOf(ExerciseType.PLYOMETRIC),
                excludedNamePatterns = listOf(
                    "jump", "crunch", "sit up", "forward fold",
                    "toe touch", "twisting"
                ),
                excludedMuscles = emptySet(),
                excludedForceTypes = emptySet()
            )

            PhysicalLimitation.PREGNANCY -> ExclusionCriteria(
                excludedTypes = setOf(ExerciseType.PLYOMETRIC),
                excludedNamePatterns = listOf(
                    "lying on back", "supine", "crunch", "sit up",
                    "jump", "contact sport", "hot yoga"
                ),
                excludedMuscles = emptySet(),
                excludedForceTypes = emptySet()
            )

            PhysicalLimitation.HIGH_BLOOD_PRESSURE -> ExclusionCriteria(
                excludedTypes = setOf(ExerciseType.ISOMETRIC),
                excludedNamePatterns = listOf(
                    "plank", "wall sit", "static hold",
                    "heavy deadlift", "heavy squat", "valsalva"
                ),
                excludedMuscles = emptySet(),
                excludedForceTypes = emptySet()
            )
        }
    }

    /**
     * Get related muscle groups that might need additional exclusion for severe cases.
     */
    private fun getRelatedMuscles(limitation: PhysicalLimitation): Set<MuscleGroup> {
        return when (limitation) {
            PhysicalLimitation.ACL_REPAIR,
            PhysicalLimitation.MENISCUS_REPAIR,
            PhysicalLimitation.KNEE_ARTHRITIS,
            PhysicalLimitation.PATELLA_TENDINITIS -> setOf(
                MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS
            )

            PhysicalLimitation.SHOULDER_BURSITIS,
            PhysicalLimitation.ROTATOR_CUFF_INJURY,
            PhysicalLimitation.SHOULDER_IMPINGEMENT,
            PhysicalLimitation.LABRUM_TEAR,
            PhysicalLimitation.FROZEN_SHOULDER -> setOf(MuscleGroup.SHOULDERS)

            PhysicalLimitation.LOWER_BACK_PAIN,
            PhysicalLimitation.HERNIATED_DISC,
            PhysicalLimitation.SCIATICA,
            PhysicalLimitation.SPONDYLOLISTHESIS -> setOf(
                MuscleGroup.BACK, MuscleGroup.CORE
            )

            PhysicalLimitation.HIP_BURSITIS,
            PhysicalLimitation.HIP_LABRUM_TEAR,
            PhysicalLimitation.HIP_REPLACEMENT -> setOf(
                MuscleGroup.GLUTES, MuscleGroup.QUADRICEPS
            )

            else -> emptySet()
        }
    }
}

/**
 * Criteria for excluding exercises based on a physical limitation.
 */
data class ExclusionCriteria(
    val excludedTypes: Set<ExerciseType>,
    val excludedNamePatterns: List<String>,
    val excludedMuscles: Set<MuscleGroup>,
    val excludedForceTypes: Set<ForceType>
)
