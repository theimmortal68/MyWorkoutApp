package com.workoutgen.ui.screens.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.workoutgen.domain.model.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GenerateWorkoutScreen(
    onWorkoutGenerated: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: GenerateWorkoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(uiState.generatedWorkoutId) {
        uiState.generatedWorkoutId?.let { workoutId ->
            onWorkoutGenerated(workoutId)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Generate Workout") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isGenerating) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Generating your workout...")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Fitness Goal Selection
                item {
                    Text(
                        text = "What's your goal?",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(FitnessGoal.entries) { goal ->
                            FilterChip(
                                selected = goal == uiState.selectedGoal,
                                onClick = { viewModel.setGoal(goal) },
                                label = { Text(goal.toDisplayName()) },
                                leadingIcon = if (goal == uiState.selectedGoal) {
                                    { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                                } else null
                            )
                        }
                    }
                }
                
                // Target Muscle Groups
                item {
                    Text(
                        text = "Target muscles (optional)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MuscleGroup.entries.filter { it != MuscleGroup.FULL_BODY }.forEach { muscle ->
                            FilterChip(
                                selected = muscle in uiState.selectedMuscles,
                                onClick = { viewModel.toggleMuscle(muscle) },
                                label = { Text(muscle.toDisplayName()) }
                            )
                        }
                    }
                }
                
                // Duration Slider
                item {
                    Text(
                        text = "Workout duration",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Timer, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${uiState.durationMinutes} min")
                    }
                    Slider(
                        value = uiState.durationMinutes.toFloat(),
                        onValueChange = { viewModel.setDuration(it.toInt()) },
                        valueRange = 15f..90f,
                        steps = 14
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("15 min", style = MaterialTheme.typography.bodySmall)
                        Text("90 min", style = MaterialTheme.typography.bodySmall)
                    }
                }
                
                // Workout Options
                item {
                    Text(
                        text = "Options",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Include warmup")
                        Switch(
                            checked = uiState.includeWarmup,
                            onCheckedChange = { viewModel.setIncludeWarmup(it) }
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Include cooldown")
                        Switch(
                            checked = uiState.includeCooldown,
                            onCheckedChange = { viewModel.setIncludeCooldown(it) }
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Prioritize compound exercises")
                        Switch(
                            checked = uiState.prioritizeCompound,
                            onCheckedChange = { viewModel.setPrioritizeCompound(it) }
                        )
                    }
                }
                
                // Error message
                if (uiState.error != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = uiState.error ?: "",
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
                
                // Generate Button
                item {
                    Button(
                        onClick = { viewModel.generateWorkout() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isGenerating
                    ) {
                        Icon(Icons.Default.AutoAwesome, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate Workout")
                    }
                }
                
                // Spacer
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

// Extension functions for display names
fun FitnessGoal.toDisplayName(): String = when (this) {
    FitnessGoal.BUILD_MUSCLE -> "Build Muscle"
    FitnessGoal.LOSE_WEIGHT -> "Lose Weight"
    FitnessGoal.BUILD_STRENGTH -> "Build Strength"
    FitnessGoal.IMPROVE_ENDURANCE -> "Endurance"
    FitnessGoal.INCREASE_FLEXIBILITY -> "Flexibility"
    FitnessGoal.GENERAL_FITNESS -> "General Fitness"
    FitnessGoal.ATHLETIC_PERFORMANCE -> "Athletic Performance"
    FitnessGoal.POWERBUILDING -> "Powerbuilding"
}

fun MuscleGroup.toDisplayName(): String = name.lowercase()
    .replace('_', ' ')
    .replaceFirstChar { it.uppercase() }
