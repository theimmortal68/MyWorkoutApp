package com.workoutgen.ui.screens.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.workoutgen.domain.model.SetLog
import com.workoutgen.domain.model.WorkoutExercise
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    workoutId: String,
    onWorkoutComplete: () -> Unit,
    onBack: () -> Unit,
    viewModel: ActiveWorkoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFinishDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(workoutId) {
        viewModel.loadWorkout(workoutId)
    }
    
    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            onWorkoutComplete()
        }
    }
    
    // Discard confirmation dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Workout?") },
            text = { Text("Your progress will be lost. Are you sure?") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onBack()
                }) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep Training")
                }
            }
        )
    }
    
    // Finish confirmation dialog
    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("Finish Workout?") },
            text = { Text("Save your workout and log your progress?") },
            confirmButton = {
                TextButton(onClick = {
                    showFinishDialog = false
                    viewModel.finishWorkout()
                }) {
                    Text("Finish")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) {
                    Text("Continue")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = uiState.workout?.name ?: "Workout",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = formatDuration(uiState.elapsedSeconds),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showDiscardDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { showFinishDialog = true }) {
                        Text("Finish")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Rest Timer (if active)
                if (uiState.isResting) {
                    item {
                        RestTimerCard(
                            remainingSeconds = uiState.restTimeRemaining,
                            onSkip = { viewModel.skipRest() }
                        )
                    }
                }
                
                // Current exercise
                uiState.currentExercise?.let { exercise ->
                    item {
                        CurrentExerciseCard(
                            exercise = exercise,
                            exerciseIndex = uiState.currentExerciseIndex,
                            totalExercises = uiState.workout?.exercises?.size ?: 0,
                            completedSets = uiState.completedSets,
                            onSetComplete = { reps, weight ->
                                viewModel.logSet(reps, weight)
                            }
                        )
                    }
                }
                
                // Upcoming exercises
                item {
                    Text(
                        text = "Up Next",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                items(uiState.upcomingExercises) { exercise ->
                    UpcomingExerciseItem(exercise = exercise)
                }
                
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun RestTimerCard(
    remainingSeconds: Int,
    onSkip: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Rest",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatDuration(remainingSeconds),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onSkip) {
                Icon(Icons.Default.SkipNext, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Skip Rest")
            }
        }
    }
}

@Composable
private fun CurrentExerciseCard(
    exercise: WorkoutExercise,
    exerciseIndex: Int,
    totalExercises: Int,
    completedSets: List<SetLog>,
    onSetComplete: (reps: Int, weight: Float?) -> Unit
) {
    var repsInput by remember { mutableStateOf(exercise.reps.last.toString()) }
    var weightInput by remember { mutableStateOf(exercise.weight?.toString() ?: "") }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Progress indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Exercise ${exerciseIndex + 1} of $totalExercises",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Set ${completedSets.size + 1} of ${exercise.sets}",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Exercise name
            Text(
                text = exercise.exercise.name,
                style = MaterialTheme.typography.headlineSmall
            )
            
            Text(
                text = "${exercise.sets} sets × ${exercise.reps.first}-${exercise.reps.last} reps",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Completed sets
            if (completedSets.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    completedSets.forEach { set ->
                        SuggestionChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = buildString {
                                        append("${set.reps}")
                                        set.weight?.let { append(" × ${it}kg") }
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            icon = { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Input row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = repsInput,
                    onValueChange = { repsInput = it },
                    label = { Text("Reps") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text("Weight (kg)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    val reps = repsInput.toIntOrNull() ?: exercise.reps.last
                    val weight = weightInput.toFloatOrNull()
                    onSetComplete(reps, weight)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Check, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Complete Set")
            }
        }
    }
}

@Composable
private fun UpcomingExerciseItem(exercise: WorkoutExercise) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = exercise.exercise.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${exercise.sets} × ${exercise.reps.first}-${exercise.reps.last}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(minutes, secs)
}
