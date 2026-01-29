package com.workoutgen.domain.usecase

import com.workoutgen.domain.model.*
import com.workoutgen.domain.repository.ExerciseRepository
import com.workoutgen.domain.repository.UserProfileRepository
import com.workoutgen.domain.repository.WorkoutLogRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Reasons why a user might want to swap an exercise
 */
enum class SwapReason(val displayName: String, val description: String) {
    NO_EQUIPMENT("No Equipment", "The required equipment isn't available"),
    EQUIPMENT_BUSY("Equipment Busy", "Someone else is using the equipment"),
    PHYSICAL_LIMITATION("Physical Limitation", "Injury, mobility issue, or discomfort"),
    TOO_DIFFICULT("Too Difficult", "Exercise is too challenging right now"),
    TOO_EASY("Too Easy", "Need something more challenging"),
    MUSCLE_FATIGUE("Muscle Fatigue", "Target muscle is already fatigued"),
    PREFERENCE("Personal Preference", "Just want a different exercise"),
    TIME_CONSTRAINT("Time Constraint", "Need a quicker exercise"),
    VARIETY("Variety", "Want to try something different"),
    OTHER("Other", "Other reason")
}

/**
 * Data class for tracking exercise swaps
 */
data class ExerciseSwap(
    val workoutId: String,
    val originalExerciseId: String,
    val originalExerciseName: String,
    val newExerciseId: String,
    val newExerciseName: String,
    val reason: SwapReason,
    val customReason: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * A recommended replacement exercise with score
 */
data class ReplacementRecommendation(
    val exercise: Exercise,
    val matchScore: Float,        // 0-100, higher is better match
    val reasons: List<String>,    // Why this is a good replacement
    val warnings: List<String> = emptyList()  // Any concerns
)

/**
 * Filter criteria for exercises
 */
data class ExerciseFilter(
    val muscleGroups: List<MuscleGroup>? = null,
    val equipment: Set<Equipment>? = null,
    val maxDifficulty: Difficulty? = null,
    val minDifficulty: Difficulty? = null,
    val exerciseTypes: List<ExerciseType>? = null,
    val excludeIds: List<String> = emptyList(),
    val searchQuery: String? = null,
    val requireCompound: Boolean? = null,
    val requireBodyweight: Boolean? = null
)

/**
 * Use case for finding exercise replacements and managing swaps
 */
class ExerciseSwapManager @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val userProfileRepository: UserProfileRepository,
    private val workoutLogRepository: WorkoutLogRepository
) {
    
    /**
     * Find the best replacement exercises for a given exercise
     */
    suspend fun findReplacements(
        originalExercise: Exercise,
        swapReason: SwapReason,
        maxResults: Int = 10
    ): List<ReplacementRecommendation> {
        val profile = userProfileRepository.getUserProfile().first()
        val allExercises = exerciseRepository.getAllExercises().first()
        
        // Build filter based on swap reason
        val filter = buildFilterForReason(originalExercise, swapReason, profile)
        
        // Get candidate exercises
        val candidates = filterExercises(allExercises, filter)
            .filter { it.id != originalExercise.id }
        
        // Score and rank candidates
        val recommendations = candidates.map { candidate ->
            scoreReplacement(originalExercise, candidate, swapReason, profile)
        }.sortedByDescending { it.matchScore }
        
        return recommendations.take(maxResults)
    }
    
    /**
     * Filter exercises based on criteria
     */
    suspend fun filterExercises(filter: ExerciseFilter): List<Exercise> {
        val allExercises = exerciseRepository.getAllExercises().first()
        return filterExercises(allExercises, filter)
    }
    
    private fun filterExercises(exercises: List<Exercise>, filter: ExerciseFilter): List<Exercise> {
        return exercises.filter { exercise ->
            // Muscle group filter
            val matchesMuscle = filter.muscleGroups?.let { muscles ->
                exercise.primaryMuscles.any { it in muscles } ||
                exercise.secondaryMuscles.any { it in muscles }
            } ?: true
            
            // Equipment filter
            val matchesEquipment = filter.equipment?.let { available ->
                exercise.equipmentRequired.all { it in available || it == Equipment.NONE }
            } ?: true
            
            // Difficulty filter (max)
            val matchesMaxDifficulty = filter.maxDifficulty?.let { maxDiff ->
                exercise.difficulty.ordinal <= maxDiff.ordinal
            } ?: true
            
            // Difficulty filter (min)
            val matchesMinDifficulty = filter.minDifficulty?.let { minDiff ->
                exercise.difficulty.ordinal >= minDiff.ordinal
            } ?: true
            
            // Exercise type filter
            val matchesType = filter.exerciseTypes?.let { types ->
                exercise.type in types
            } ?: true
            
            // Exclusion filter
            val notExcluded = exercise.id !in filter.excludeIds
            
            // Search query
            val matchesSearch = filter.searchQuery?.let { query ->
                val lowerQuery = query.lowercase()
                exercise.name.lowercase().contains(lowerQuery) ||
                exercise.description.lowercase().contains(lowerQuery) ||
                exercise.primaryMuscles.any { it.displayName.lowercase().contains(lowerQuery) }
            } ?: true
            
            // Compound requirement
            val matchesCompound = filter.requireCompound?.let { require ->
                if (require) exercise.type == ExerciseType.COMPOUND else true
            } ?: true
            
            // Bodyweight requirement
            val matchesBodyweight = filter.requireBodyweight?.let { require ->
                if (require) {
                    exercise.equipmentRequired.isEmpty() || 
                    exercise.equipmentRequired == listOf(Equipment.NONE)
                } else true
            } ?: true
            
            matchesMuscle && matchesEquipment && matchesMaxDifficulty && 
            matchesMinDifficulty && matchesType && notExcluded && 
            matchesSearch && matchesCompound && matchesBodyweight
        }
    }
    
    /**
     * Build appropriate filter based on swap reason
     */
    private fun buildFilterForReason(
        original: Exercise,
        reason: SwapReason,
        profile: UserProfile?
    ): ExerciseFilter {
        val baseFilter = ExerciseFilter(
            muscleGroups = original.primaryMuscles,
            equipment = profile?.availableEquipment,
            excludeIds = profile?.excludedExercises ?: emptyList()
        )
        
        return when (reason) {
            SwapReason.NO_EQUIPMENT, SwapReason.EQUIPMENT_BUSY -> {
                // Find exercises that don't use the same equipment
                val problematicEquipment = original.equipmentRequired
                baseFilter.copy(
                    equipment = profile?.availableEquipment?.minus(problematicEquipment.toSet())
                        ?: setOf(Equipment.NONE)
                )
            }
            
            SwapReason.PHYSICAL_LIMITATION -> {
                // Prefer easier exercises, avoid high-impact
                baseFilter.copy(
                    maxDifficulty = Difficulty.INTERMEDIATE,
                    exerciseTypes = listOf(ExerciseType.ISOLATION, ExerciseType.STRETCHING)
                )
            }
            
            SwapReason.TOO_DIFFICULT -> {
                // Find easier versions
                val easierDifficulty = when (original.difficulty) {
                    Difficulty.EXPERT -> Difficulty.ADVANCED
                    Difficulty.ADVANCED -> Difficulty.INTERMEDIATE
                    Difficulty.INTERMEDIATE -> Difficulty.BEGINNER
                    Difficulty.BEGINNER -> Difficulty.BEGINNER
                }
                baseFilter.copy(maxDifficulty = easierDifficulty)
            }
            
            SwapReason.TOO_EASY -> {
                // Find harder versions
                val harderDifficulty = when (original.difficulty) {
                    Difficulty.BEGINNER -> Difficulty.INTERMEDIATE
                    Difficulty.INTERMEDIATE -> Difficulty.ADVANCED
                    Difficulty.ADVANCED -> Difficulty.EXPERT
                    Difficulty.EXPERT -> Difficulty.EXPERT
                }
                baseFilter.copy(minDifficulty = harderDifficulty)
            }
            
            SwapReason.MUSCLE_FATIGUE -> {
                // Find exercises for secondary muscles instead
                baseFilter.copy(
                    muscleGroups = original.secondaryMuscles.takeIf { it.isNotEmpty() }
                        ?: original.primaryMuscles
                )
            }
            
            SwapReason.TIME_CONSTRAINT -> {
                // Prefer isolation exercises (typically faster)
                baseFilter.copy(
                    exerciseTypes = listOf(ExerciseType.ISOLATION, ExerciseType.COMPOUND)
                )
            }
            
            else -> baseFilter
        }
    }
    
    /**
     * Score how well a replacement matches the original
     */
    private fun scoreReplacement(
        original: Exercise,
        replacement: Exercise,
        reason: SwapReason,
        profile: UserProfile?
    ): ReplacementRecommendation {
        var score = 0f
        val reasons = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // Primary muscle match (most important)
        val primaryMatch = original.primaryMuscles.intersect(replacement.primaryMuscles.toSet())
        score += primaryMatch.size * 25f
        if (primaryMatch.isNotEmpty()) {
            reasons.add("Targets same primary muscles: ${primaryMatch.joinToString { it.displayName }}")
        }
        
        // Secondary muscle match
        val secondaryMatch = original.secondaryMuscles.intersect(replacement.secondaryMuscles.toSet())
        score += secondaryMatch.size * 10f
        if (secondaryMatch.isNotEmpty()) {
            reasons.add("Also works ${secondaryMatch.joinToString { it.displayName }}")
        }
        
        // Same exercise type (compound/isolation)
        if (original.type == replacement.type) {
            score += 15f
            reasons.add("Same movement type (${replacement.type.name.lowercase()})")
        }
        
        // Similar difficulty
        val diffDiff = kotlin.math.abs(original.difficulty.ordinal - replacement.difficulty.ordinal)
        score += (3 - diffDiff) * 5f
        if (diffDiff == 0) {
            reasons.add("Same difficulty level")
        } else if (diffDiff == 1) {
            if (replacement.difficulty.ordinal < original.difficulty.ordinal) {
                reasons.add("Slightly easier alternative")
            } else {
                reasons.add("Slightly more challenging")
            }
        }
        
        // Equipment availability bonus
        val hasEquipment = profile?.availableEquipment?.let { available ->
            replacement.equipmentRequired.all { it in available || it == Equipment.NONE }
        } ?: true
        if (hasEquipment) {
            score += 10f
            if (replacement.equipmentRequired.isEmpty() || 
                replacement.equipmentRequired == listOf(Equipment.NONE)) {
                reasons.add("No equipment needed")
            } else {
                reasons.add("Equipment available")
            }
        } else {
            warnings.add("May require equipment you don't have")
        }
        
        // Reason-specific scoring
        when (reason) {
            SwapReason.NO_EQUIPMENT, SwapReason.EQUIPMENT_BUSY -> {
                if (replacement.equipmentRequired != original.equipmentRequired) {
                    score += 15f
                    reasons.add("Uses different equipment")
                }
                if (replacement.equipmentRequired.isEmpty() || 
                    replacement.equipmentRequired == listOf(Equipment.NONE)) {
                    score += 10f
                }
            }
            
            SwapReason.PHYSICAL_LIMITATION -> {
                if (replacement.difficulty.ordinal < original.difficulty.ordinal) {
                    score += 10f
                }
                if (replacement.type == ExerciseType.ISOLATION) {
                    score += 5f
                    reasons.add("Lower joint stress")
                }
            }
            
            SwapReason.TOO_DIFFICULT -> {
                if (replacement.difficulty.ordinal < original.difficulty.ordinal) {
                    score += 15f
                    reasons.add("Easier progression")
                }
            }
            
            SwapReason.TOO_EASY -> {
                if (replacement.difficulty.ordinal > original.difficulty.ordinal) {
                    score += 15f
                    reasons.add("More challenging")
                }
            }
            
            SwapReason.VARIETY -> {
                // Prefer exercises that are different but target same muscles
                if (replacement.name != original.name) {
                    score += 5f
                }
            }
            
            else -> {}
        }
        
        // Cap score at 100
        score = score.coerceIn(0f, 100f)
        
        return ReplacementRecommendation(
            exercise = replacement,
            matchScore = score,
            reasons = reasons,
            warnings = warnings
        )
    }
    
    /**
     * Get the best single replacement for quick swap
     */
    suspend fun getBestReplacement(
        originalExercise: Exercise,
        swapReason: SwapReason
    ): Exercise? {
        return findReplacements(originalExercise, swapReason, 1).firstOrNull()?.exercise
    }
    
    /**
     * Track an exercise swap for analytics and learning
     */
    suspend fun recordSwap(swap: ExerciseSwap) {
        // In a full implementation, this would save to the database
        // for learning user preferences over time
        workoutLogRepository.recordExerciseSwap(swap)
    }
    
    /**
     * Get exercises the user frequently swaps away from
     * (helps identify exercises to avoid in future generations)
     */
    suspend fun getFrequentlySwappedExercises(): List<Pair<String, Int>> {
        return workoutLogRepository.getSwapStatistics()
    }
}

/**
 * Extension function to add swap tracking to WorkoutLogRepository
 */
suspend fun WorkoutLogRepository.recordExerciseSwap(swap: ExerciseSwap) {
    // Default implementation - override in actual repository
}

suspend fun WorkoutLogRepository.getSwapStatistics(): List<Pair<String, Int>> {
    // Default implementation - override in actual repository
    return emptyList()
}
