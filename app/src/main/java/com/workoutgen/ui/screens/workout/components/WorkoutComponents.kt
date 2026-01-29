package com.workoutgen.ui.screens.workout.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.workoutgen.domain.model.Exercise
import com.workoutgen.domain.usecase.ReplacementRecommendation
import com.workoutgen.domain.usecase.SwapReason

// ============================================================================
// EXERCISE SWAP BOTTOM SHEET
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSwapSheet(
    currentExercise: Exercise,
    recommendations: List<ReplacementRecommendation>,
    isLoading: Boolean,
    selectedReason: SwapReason?,
    onReasonSelected: (SwapReason) -> Unit,
    onExerciseSelected: (Exercise) -> Unit,
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(if (selectedReason != null) 2 else 1) }
    
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Text(
                "Swap Exercise",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                "Replace: ${currentExercise.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Step indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                StepIndicator(step = 1, currentStep = step, label = "Reason")
                Spacer(modifier = Modifier.width(8.dp))
                HorizontalDivider(
                    modifier = Modifier
                        .width(32.dp)
                        .align(Alignment.CenterVertically)
                )
                Spacer(modifier = Modifier.width(8.dp))
                StepIndicator(step = 2, currentStep = step, label = "Select")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            when (step) {
                1 -> ReasonSelectionStep(
                    selectedReason = selectedReason,
                    onReasonSelected = { reason ->
                        onReasonSelected(reason)
                        step = 2
                    }
                )
                2 -> ExerciseSelectionStep(
                    recommendations = recommendations,
                    isLoading = isLoading,
                    onExerciseSelected = onExerciseSelected,
                    onBack = { step = 1 }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StepIndicator(step: Int, currentStep: Int, label: String) {
    val isActive = currentStep >= step
    val color = if (isActive) MaterialTheme.colorScheme.primary 
                else MaterialTheme.colorScheme.outline
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(50),
            color = if (isActive) color else Color.Transparent,
            border = if (!isActive) ButtonDefaults.outlinedButtonBorder else null,
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    "$step",
                    color = if (isActive) MaterialTheme.colorScheme.onPrimary else color,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun ReasonSelectionStep(
    selectedReason: SwapReason?,
    onReasonSelected: (SwapReason) -> Unit
) {
    Text(
        "Why do you want to swap?",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Medium
    )
    
    Text(
        "This helps us recommend better alternatives",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline
    )
    
    Spacer(modifier = Modifier.height(16.dp))
    
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.heightIn(max = 400.dp)
    ) {
        items(SwapReason.entries.toList()) { reason ->
            SwapReasonCard(
                reason = reason,
                isSelected = selectedReason == reason,
                onClick = { onReasonSelected(reason) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwapReasonCard(
    reason: SwapReason,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val icon = when (reason) {
        SwapReason.NO_EQUIPMENT -> Icons.Outlined.DoNotDisturbOn
        SwapReason.EQUIPMENT_BUSY -> Icons.Outlined.HourglassEmpty
        SwapReason.PHYSICAL_LIMITATION -> Icons.Outlined.Healing
        SwapReason.TOO_DIFFICULT -> Icons.Outlined.TrendingDown
        SwapReason.TOO_EASY -> Icons.Outlined.TrendingUp
        SwapReason.MUSCLE_FATIGUE -> Icons.Outlined.Battery2Bar
        SwapReason.PREFERENCE -> Icons.Outlined.ThumbUp
        SwapReason.TIME_CONSTRAINT -> Icons.Outlined.Timer
        SwapReason.VARIETY -> Icons.Outlined.Shuffle
        SwapReason.OTHER -> Icons.Outlined.MoreHoriz
    }
    
    Card(
        onClick = onClick,
        colors = if (isSelected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary 
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    reason.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    reason.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ExerciseSelectionStep(
    recommendations: List<ReplacementRecommendation>,
    isLoading: Boolean,
    onExerciseSelected: (Exercise) -> Unit,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, null, Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Back")
        }
        
        Text(
            "${recommendations.size} alternatives",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Finding alternatives...")
            }
        }
    } else if (recommendations.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.SearchOff,
                    null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("No alternatives found")
                Text(
                    "Try a different reason",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.heightIn(max = 400.dp)
        ) {
            items(recommendations) { recommendation ->
                RecommendationCard(
                    recommendation = recommendation,
                    onClick = { onExerciseSelected(recommendation.exercise) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecommendationCard(
    recommendation: ReplacementRecommendation,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        recommendation.exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        recommendation.exercise.primaryMuscles.joinToString { it.displayName },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                
                // Match score badge
                MatchScoreBadge(score = recommendation.matchScore)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Reasons why this is a good match
            recommendation.reasons.take(2).forEach { reason ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        Icons.Default.Check,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        reason,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            // Warnings
            recommendation.warnings.forEach { warning ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFFF9800)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        warning,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchScoreBadge(score: Float) {
    val color = when {
        score >= 80 -> Color(0xFF4CAF50)
        score >= 60 -> Color(0xFF8BC34A)
        score >= 40 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
    
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            "${score.toInt()}%",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

// ============================================================================
// SET FAILURE DIALOG
// ============================================================================

@Composable
fun SetFailureDialog(
    targetReps: Int,
    achievedReps: Int,
    onReasonSelected: (com.workoutgen.domain.usecase.FailureReason) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Info, null) },
        title = { Text("Missed Target Reps") },
        text = {
            Column {
                Text("You completed $achievedReps of $targetReps reps.")
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "What happened?",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                val commonReasons = listOf(
                    com.workoutgen.domain.usecase.FailureReason.STRENGTH_FAILURE,
                    com.workoutgen.domain.usecase.FailureReason.FORM_BREAKDOWN,
                    com.workoutgen.domain.usecase.FailureReason.FATIGUE,
                    com.workoutgen.domain.usecase.FailureReason.PAIN_DISCOMFORT
                )
                
                commonReasons.forEach { reason ->
                    TextButton(
                        onClick = { onReasonSelected(reason) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(reason.displayName, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { 
                onReasonSelected(com.workoutgen.domain.usecase.FailureReason.OTHER) 
            }) {
                Text("Skip")
            }
        }
    )
}

// ============================================================================
// ADJUSTMENT SUGGESTION CARD
// ============================================================================

@Composable
fun AdjustmentSuggestionCard(
    adjustment: com.workoutgen.domain.usecase.WorkoutAdjustment,
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Lightbulb,
                    null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Suggested Adjustment",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val (icon, message) = when (adjustment) {
                is com.workoutgen.domain.usecase.WorkoutAdjustment.ReduceWeight -> 
                    Icons.Default.TrendingDown to "Reduce weight to ${adjustment.newWeight}kg (${adjustment.reason})"
                is com.workoutgen.domain.usecase.WorkoutAdjustment.ReduceReps -> 
                    Icons.Default.Remove to "Adjust target to ${adjustment.newTargetReps} reps (${adjustment.reason})"
                is com.workoutgen.domain.usecase.WorkoutAdjustment.AddRest -> 
                    Icons.Default.Timer to "Take ${adjustment.additionalSeconds}s extra rest (${adjustment.reason})"
                is com.workoutgen.domain.usecase.WorkoutAdjustment.SwapExercise -> 
                    Icons.Default.SwapHoriz to adjustment.reason
                is com.workoutgen.domain.usecase.WorkoutAdjustment.EndExerciseEarly -> 
                    Icons.Default.SkipNext to adjustment.reason
                is com.workoutgen.domain.usecase.WorkoutAdjustment.DropSet -> 
                    Icons.Default.SkipNext to adjustment.reason
                com.workoutgen.domain.usecase.WorkoutAdjustment.NoAdjustment -> 
                    Icons.Default.Check to "Keep going!"
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(message, style = MaterialTheme.typography.bodyMedium)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Ignore")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onAccept) {
                    Text("Apply")
                }
            }
        }
    }
}
