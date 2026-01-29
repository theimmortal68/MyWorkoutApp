package com.workoutgen.domain.usecase

import com.workoutgen.domain.model.*
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Reasons for failing to complete prescribed reps
 */
enum class FailureReason(val displayName: String, val description: String) {
    STRENGTH_FAILURE("Strength Limit", "Muscles gave out before completing reps"),
    FORM_BREAKDOWN("Form Breakdown", "Had to stop to maintain proper form"),
    FATIGUE("General Fatigue", "Too tired to continue safely"),
    PAIN_DISCOMFORT("Pain/Discomfort", "Felt pain or discomfort"),
    EQUIPMENT_ISSUE("Equipment Issue", "Problem with equipment"),
    TIME_CONSTRAINT("Time Constraint", "Running out of time"),
    MENTAL("Mental/Focus", "Lost focus or motivation"),
    OTHER("Other", "Other reason")
}

/**
 * Result of a completed set with performance data
 */
data class SetResult(
    val exerciseId: String,
    val setNumber: Int,
    val targetReps: Int,
    val achievedReps: Int,
    val targetWeight: Float?,
    val actualWeight: Float?,
    val rpe: Int? = null,           // 1-10 scale
    val rir: Int? = null,           // Reps in reserve
    val failureReason: FailureReason? = null,
    val notes: String? = null,
    val isWarmup: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    val completedAsPlanned: Boolean
        get() = achievedReps >= targetReps
    
    val repShortfall: Int
        get() = (targetReps - achievedReps).coerceAtLeast(0)
    
    val performanceRatio: Float
        get() = if (targetReps > 0) achievedReps.toFloat() / targetReps else 1f
}

/**
 * Adjustment recommendation for remaining sets/exercises
 */
sealed class WorkoutAdjustment {
    data class ReduceWeight(
        val newWeight: Float,
        val percentageReduction: Float,
        val reason: String
    ) : WorkoutAdjustment()
    
    data class ReduceReps(
        val newTargetReps: Int,
        val reason: String
    ) : WorkoutAdjustment()
    
    data class AddRest(
        val additionalSeconds: Int,
        val reason: String
    ) : WorkoutAdjustment()
    
    data class DropSet(
        val reason: String
    ) : WorkoutAdjustment()
    
    data class SwapExercise(
        val reason: String,
        val suggestedSwapReason: SwapReason
    ) : WorkoutAdjustment()
    
    data class EndExerciseEarly(
        val completedSets: Int,
        val reason: String
    ) : WorkoutAdjustment()
    
    object NoAdjustment : WorkoutAdjustment()
}

/**
 * Real-time workout adjustment engine
 * Analyzes performance and suggests adjustments to optimize the workout
 */
class WorkoutAdjustmentEngine @Inject constructor() {
    
    // Track set results within current workout
    private val sessionResults = mutableListOf<SetResult>()
    
    /**
     * Record a completed set and get adjustment recommendations
     */
    fun recordSet(result: SetResult): List<WorkoutAdjustment> {
        sessionResults.add(result)
        return analyzeAndRecommend(result)
    }
    
    /**
     * Analyze the set result and recommend adjustments
     */
    private fun analyzeAndRecommend(result: SetResult): List<WorkoutAdjustment> {
        val recommendations = mutableListOf<WorkoutAdjustment>()
        
        if (result.isWarmup || result.completedAsPlanned) {
            // Warmup or successful set - check if we should increase
            if (!result.isWarmup && result.rpe != null && result.rpe <= 6) {
                // Too easy - could potentially increase (but don't force it)
            }
            return listOf(WorkoutAdjustment.NoAdjustment)
        }
        
        // Failed set - determine appropriate adjustment
        val exerciseHistory = sessionResults.filter { it.exerciseId == result.exerciseId && !it.isWarmup }
        val consecutiveFailures = exerciseHistory.takeLastWhile { !it.completedAsPlanned }.size
        val totalFailures = exerciseHistory.count { !it.completedAsPlanned }
        
        when {
            // Pain/discomfort - prioritize safety
            result.failureReason == FailureReason.PAIN_DISCOMFORT -> {
                recommendations.add(
                    WorkoutAdjustment.SwapExercise(
                        reason = "Experiencing discomfort - recommend switching to an alternative",
                        suggestedSwapReason = SwapReason.PHYSICAL_LIMITATION
                    )
                )
            }
            
            // Multiple consecutive failures - significant adjustment needed
            consecutiveFailures >= 3 -> {
                recommendations.add(
                    WorkoutAdjustment.EndExerciseEarly(
                        completedSets = exerciseHistory.size,
                        reason = "Multiple failed sets - moving on to preserve workout quality"
                    )
                )
            }
            
            // Two consecutive failures - reduce weight significantly
            consecutiveFailures == 2 -> {
                val currentWeight = result.actualWeight ?: result.targetWeight
                if (currentWeight != null && currentWeight > 0) {
                    val newWeight = calculateReducedWeight(currentWeight, 0.15f) // 15% reduction
                    recommendations.add(
                        WorkoutAdjustment.ReduceWeight(
                            newWeight = newWeight,
                            percentageReduction = 15f,
                            reason = "Two consecutive failures - reducing weight by 15%"
                        )
                    )
                }
                recommendations.add(
                    WorkoutAdjustment.AddRest(
                        additionalSeconds = 60,
                        reason = "Take extra rest to recover"
                    )
                )
            }
            
            // First failure - minor adjustments based on shortfall
            else -> {
                when {
                    // Missed by 1-2 reps - slight adjustment
                    result.repShortfall <= 2 -> {
                        recommendations.add(
                            WorkoutAdjustment.AddRest(
                                additionalSeconds = 30,
                                reason = "Missed by ${result.repShortfall} rep(s) - adding 30s rest"
                            )
                        )
                        // Optionally suggest dropping 1-2 reps from target
                        if (result.rpe != null && result.rpe >= 10) {
                            recommendations.add(
                                WorkoutAdjustment.ReduceReps(
                                    newTargetReps = result.targetReps - 1,
                                    reason = "Adjust target to ${result.targetReps - 1} reps for remaining sets"
                                )
                            )
                        }
                    }
                    
                    // Missed by 3+ reps - reduce weight
                    result.repShortfall >= 3 -> {
                        val currentWeight = result.actualWeight ?: result.targetWeight
                        if (currentWeight != null && currentWeight > 0) {
                            val reductionPercent = when {
                                result.repShortfall >= 5 -> 0.15f
                                result.repShortfall >= 3 -> 0.10f
                                else -> 0.05f
                            }
                            val newWeight = calculateReducedWeight(currentWeight, reductionPercent)
                            recommendations.add(
                                WorkoutAdjustment.ReduceWeight(
                                    newWeight = newWeight,
                                    percentageReduction = reductionPercent * 100,
                                    reason = "Missed by ${result.repShortfall} reps - reducing weight by ${(reductionPercent * 100).toInt()}%"
                                )
                            )
                        } else {
                            recommendations.add(
                                WorkoutAdjustment.ReduceReps(
                                    newTargetReps = result.achievedReps,
                                    reason = "Adjust target to ${result.achievedReps} reps for remaining sets"
                                )
                            )
                        }
                    }
                }
            }
        }
        
        // Handle specific failure reasons
        when (result.failureReason) {
            FailureReason.FORM_BREAKDOWN -> {
                recommendations.add(0, WorkoutAdjustment.ReduceWeight(
                    newWeight = calculateReducedWeight(result.actualWeight ?: 0f, 0.10f),
                    percentageReduction = 10f,
                    reason = "Form breakdown - reduce weight to maintain technique"
                ))
            }
            
            FailureReason.FATIGUE -> {
                recommendations.add(
                    WorkoutAdjustment.AddRest(
                        additionalSeconds = 90,
                        reason = "Fatigue detected - take extended rest"
                    )
                )
            }
            
            FailureReason.EQUIPMENT_ISSUE -> {
                recommendations.add(
                    WorkoutAdjustment.SwapExercise(
                        reason = "Equipment issue - consider alternative exercise",
                        suggestedSwapReason = SwapReason.EQUIPMENT_BUSY
                    )
                )
            }
            
            FailureReason.TIME_CONSTRAINT -> {
                recommendations.add(
                    WorkoutAdjustment.DropSet(
                        reason = "Time constraint - skip remaining sets if needed"
                    )
                )
            }
            
            else -> {}
        }
        
        return recommendations.ifEmpty { listOf(WorkoutAdjustment.NoAdjustment) }
    }
    
    /**
     * Get recommended weight for next set based on session history
     */
    fun getRecommendedWeight(
        exerciseId: String,
        originalWeight: Float,
        targetReps: Int
    ): Float {
        val exerciseResults = sessionResults
            .filter { it.exerciseId == exerciseId && !it.isWarmup }
        
        if (exerciseResults.isEmpty()) return originalWeight
        
        val lastResult = exerciseResults.last()
        
        return when {
            // Last set was successful and felt easy
            lastResult.completedAsPlanned && (lastResult.rpe ?: 8) <= 6 -> {
                // Could increase slightly, but be conservative
                originalWeight
            }
            
            // Last set failed significantly
            !lastResult.completedAsPlanned && lastResult.repShortfall >= 3 -> {
                calculateReducedWeight(originalWeight, 0.10f)
            }
            
            // Last set barely failed
            !lastResult.completedAsPlanned && lastResult.repShortfall <= 2 -> {
                calculateReducedWeight(originalWeight, 0.05f)
            }
            
            else -> originalWeight
        }
    }
    
    /**
     * Get recommended reps for next set
     */
    fun getRecommendedReps(
        exerciseId: String,
        originalReps: Int
    ): Int {
        val exerciseResults = sessionResults
            .filter { it.exerciseId == exerciseId && !it.isWarmup }
        
        if (exerciseResults.isEmpty()) return originalReps
        
        // Calculate average achieved reps
        val avgAchieved = exerciseResults.map { it.achievedReps }.average()
        
        return when {
            avgAchieved < originalReps - 2 -> (avgAchieved + 1).toInt().coerceAtLeast(1)
            else -> originalReps
        }
    }
    
    /**
     * Calculate volume completed vs planned
     */
    fun getVolumeCompletion(exerciseId: String): VolumeCompletion {
        val results = sessionResults.filter { it.exerciseId == exerciseId && !it.isWarmup }
        
        if (results.isEmpty()) return VolumeCompletion(0, 0, 0f, 0f)
        
        val plannedVolume = results.sumOf { 
            (it.targetWeight ?: 0f) * it.targetReps.toDouble() 
        }.toFloat()
        
        val achievedVolume = results.sumOf { 
            (it.actualWeight ?: 0f) * it.achievedReps.toDouble() 
        }.toFloat()
        
        return VolumeCompletion(
            plannedSets = results.size,
            completedSets = results.count { it.completedAsPlanned },
            plannedVolume = plannedVolume,
            achievedVolume = achievedVolume
        )
    }
    
    /**
     * Get overall workout performance summary
     */
    fun getWorkoutSummary(): WorkoutPerformanceSummary {
        val workingSets = sessionResults.filter { !it.isWarmup }
        
        if (workingSets.isEmpty()) {
            return WorkoutPerformanceSummary(
                totalSets = 0,
                completedSets = 0,
                failedSets = 0,
                completionRate = 1f,
                averageRpe = null,
                totalVolume = 0f,
                adjustmentsMade = 0
            )
        }
        
        val completed = workingSets.count { it.completedAsPlanned }
        val avgRpe = workingSets.mapNotNull { it.rpe }.takeIf { it.isNotEmpty() }?.average()?.toFloat()
        val totalVolume = workingSets.sumOf { 
            ((it.actualWeight ?: 0f) * it.achievedReps).toDouble() 
        }.toFloat()
        
        return WorkoutPerformanceSummary(
            totalSets = workingSets.size,
            completedSets = completed,
            failedSets = workingSets.size - completed,
            completionRate = completed.toFloat() / workingSets.size,
            averageRpe = avgRpe,
            totalVolume = totalVolume,
            adjustmentsMade = workingSets.count { !it.completedAsPlanned }
        )
    }
    
    /**
     * Clear session data for new workout
     */
    fun resetSession() {
        sessionResults.clear()
    }
    
    /**
     * Calculate reduced weight rounded to nearest 2.5
     */
    private fun calculateReducedWeight(weight: Float, reductionPercent: Float): Float {
        val reduced = weight * (1 - reductionPercent)
        return (reduced / 2.5f).roundToInt() * 2.5f
    }
}

data class VolumeCompletion(
    val plannedSets: Int,
    val completedSets: Int,
    val plannedVolume: Float,
    val achievedVolume: Float
) {
    val volumeCompletionRate: Float
        get() = if (plannedVolume > 0) achievedVolume / plannedVolume else 1f
    
    val setCompletionRate: Float
        get() = if (plannedSets > 0) completedSets.toFloat() / plannedSets else 1f
}

data class WorkoutPerformanceSummary(
    val totalSets: Int,
    val completedSets: Int,
    val failedSets: Int,
    val completionRate: Float,
    val averageRpe: Float?,
    val totalVolume: Float,
    val adjustmentsMade: Int
)
